/* MIT License
 *
 * Copyright (c) 2022 TKNKLA
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.tknkla.rj.examples.cli;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.LongBinaryOperator;

import com.tknkla.rj.ExecutionStrategy;
import com.tknkla.rj.RJ;
import com.tknkla.rj.SetOperator;
import com.tknkla.rj.groups.LongGroupOperator;

/**
 * A minimal command line interface for graph canonization.
 * 
 * <p>Usage: CanonizerMain [--parallel] [--] [FILE...]</p>
 * <ul>
 * <li>A graph is read from stdin if argument '--' is present.</li>
 * <li>Parallel processing is enabled if '--parallel' is present.</li>
 * <li>Any other argument is interpreted as a path; a graph is read from that source.</li>
 * <li>The order of the inputs is irrelevant as all the inputs are summed together (or interpreted as a single input).</li>
 * <li>If there is any error (IO or parsing), an exception is thrown and the process is aborted.</li>
 * </ul>
 * 
 * <p>Input (graph) format:</p>
 * <ul>
 * <li>The input format is text based and encoded in UTF-8.</li>
 * <li>All whitespace at the beginning of each line are ignored.</li>
 * <li>All empty lines and lines starting with '#' are ignored.</li>
 * <li>Every line that is not ignored is split into a set of tokens (separated by whitespace).</li>
 * <li>Token is any string (not starting with a number) optionally prefixed by a number or a minus sign (equals to -1) denoting weight of the edge. No prefix is equal to weight of 1.</li>
 * <li>For every pair of tokens in the set a pair of edges are created. For example a statement "nA mB" implies that the relation between A and B is n and the relation between B and A is m.</li>
 * <li>A unit set of tokens is interpreted as an edge withing the vertex/symbol itself. For example a line "nA" implies that relation between A and A is n.</li>
 * <li>The weights are 64-bit integers. Note that every combination of the weights must also be a valid 64-bit integer.</li>
 * </ul>
 * 
 * <p>In practice an undirected graph may be expressed simply as a list of edges with each edge (pair of vertices/symbols) on its own line.</p>
 * 
 * <p>Output (order) format:</p>
 * <ul>
 * <li>Each line of the output represents an automorphic group of symbols.</li>
 * <li>Order of the output lines expresses the truly relative order of the symbols.</li>
 * <li>Order of the symbols on each line expresses the strongly relative order of the symbols.</li>
 * </ul>
 * 
 * @author Timo Santasalo
 */
public class CanonizerMain {
	
	private static void append(List<Edge> dst, String from, String to, long v) {
		Edge ne = new Edge(from, to, v);
		int p = Collections.binarySearch(dst, ne);
		if (p<0) {
			dst.add(~p, ne);
		} else {
			dst.set(p, dst.get(p).merge(ne));
		}
	}
	
	private static void read(List<Edge> dst, String li, String src) {
		String[] ps = li.split("\\s");
		
		String[] syms = new String[ps.length];
		long[] vals = new long[ps.length];
		
		for (int i=0; i<ps.length; i++) {
			int p0 = 0;
			int p1 = 0;
			boolean neg = false;
			if (ps[i].charAt(0)=='-') {
				p0++;
				p1++;
				neg = true;
			}
			while (p1<ps.length && Character.isDigit(ps[i].charAt(p1))) {
				p1++;
			}
			if (p1==ps.length) {
				throw new RuntimeException(src+": Invalid token: '"+ps[i]+"'");
			}
			syms[i] = ps[i].substring(p1);
			vals[i] = p0==p1 ? 1 : Long.parseLong(ps[i].substring(p0, p1));
			if (neg) {
				vals[i] = -vals[i];
			}
		}
		
		if (ps.length==1) {
			append(dst, syms[0], syms[0], vals[0]);
		} else {
			for (int i=0; i<ps.length; i++) {
				for (int j=0; j<ps.length; j++) {
					if (i==j) {
						continue;
					}
					append(dst, syms[i], syms[j], vals[i]);
				}
			}
		}
	}
	
	private static void read(List<Edge> dst, InputStream in, String src) throws IOException {
		LineNumberReader lnr = new LineNumberReader(new InputStreamReader(in, "utf-8"));
		while (true) {
			String li = lnr.readLine();
			if (li==null) {
				break;
			}
			li = li.trim();
			if (li.length()==0 || li.charAt(0)=='#') {
				continue;
			}
			read(dst, li, src+":"+lnr.getLineNumber());
		}
	}

	public static void main(String[] args) throws IOException {
		
		// aggregate inputs
		List<Edge> es = new ArrayList<>();		
		for (String s : args) {
			if (s.equals("--parallel")) {
				RJ.setExecutor(ExecutionStrategy.PARALLEL);
			} else if (s.equals("--")) {
				read(es, System.in, "STDIN");
			} else {
				read(es, new FileInputStream(s), s);
			}
		}

		// aggregate list of symbols
		String[] syms = RJ.merge(String.class, es.size()<<1,
				(int p) -> {
					Edge e = es.get(p>>1);
					return new String[] { (p&0) == 0 ? e.from : e.to };
				},
				Comparator.naturalOrder(),
				(String a, String b) -> a,
				SetOperator.UNION);
		
		if (syms.length==0) {
			// no data
			return;
		}
		
		// aggregate edges
		long[][] vals = RJ.populate(syms.length, syms.length, (int p, int q) -> {
			int ep = Collections.binarySearch(es, new Edge(syms[p], syms[q], 0));
			return ep<0 ? 0l : es.get(ep).value;
		});
		
		LongBinaryOperator fg = (long a, long b) -> vals[(int)a][(int)b]; 
		
		// compute groups
		long[][] groups = RJ.groups(
				new long[][] { RJ.populate(syms.length, (int p) -> (long)p) },
				fg,
				LongGroupOperator.ADDITIVE,
				null);
		
		// compute order
		long[] order = RJ.order(groups, fg, LongGroupOperator.ADDITIVE, null);
		
		// output result
		int p = 0;
		for (int i=0; i<groups.length; i++) {
			for (int j=0; j<groups[i].length; j++) {
				if (j>0) {
					System.out.print(' ');
				}
				System.out.print(syms[(int)order[p]]);
				p++;
			}
			System.out.println();
		}
	
	}

	private static class Edge implements Comparable<Edge> {
		
		final String from;
		final String to;
		final long value;
		
		public Edge(String from, String to, long value) {
			super();
			this.from = from;
			this.to = to;
			this.value = value;
		}

		@Override
		public int compareTo(Edge o) {
			return RJ.compare(
					() -> from.compareTo(o.from),
					() -> to.compareTo(o.to));
		}
		
		public Edge merge(Edge e) {
			assert from.equals(e.from);
			assert to.equals(e.to);
			return new Edge(from, to, value+e.value);
		}
		
	}
	
}
