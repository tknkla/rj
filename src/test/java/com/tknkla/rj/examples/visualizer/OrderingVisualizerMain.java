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
package com.tknkla.rj.examples.visualizer;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.function.IntBinaryOperator;
import java.util.function.IntFunction;
import java.util.function.IntUnaryOperator;

import javax.imageio.ImageIO;

import com.tknkla.rj.RJ;

/**
 * Generates visualizations of relative ordering.
 * 
 * <p>Usage: <code>java ...OrderingVisualizerMain [output] [permutations] [vertices] [graph]</code>
 * 
 * @see RJ#order(int[][], IntBinaryOperator, com.tknkla.rj.functions.IntGroupOperator, IntBinaryOperator)
 * @author Timo Santasalo
 */
public class OrderingVisualizerMain extends AbstractOrderAlgorithm {
		
	private final List<Event> evs = new ArrayList<>();

	public OrderingVisualizerMain(int ln, IntBinaryOperator fg) {
		super(ln, fg);
		order();
	}
	
	@Override
	public String toString() {
		StringBuilder ret = new StringBuilder();
		for (Event ev : evs) {
			ret.append(ev).append('\n');
		}
		ret.append(Arrays.toString(od)).append('\n');
		ret.append(Arrays.toString(odi)).append('\n');
		return ret.toString();
	}
	
	private Event last() {
		return evs.get(evs.size()-1);
	}
	
	private Event removeLast() {
		return evs.remove(evs.size()-1);
	}
	
	@Override
	protected void beforePivot(int[][] order, int pv) {
		evs.add(new PivotEvent(pv));
		last().add(order);
		
	}

	@Override
	protected void afterPivot(int[][] order) {
		if (last() instanceof PropagateEvent) {
			PropagateEvent evp = (PropagateEvent) removeLast();
			((PivotEvent) last()).evp = evp;
		}
		last().add(order);
	}

	@Override
	protected void beforeOrdering(int[][] order) {
		evs.add(new OrderingEvent());
		last().add(order);
	}

	@Override
	protected void afterOrdering(int[][] order) {
		System.out.println(order.length+"/"+ln);
		last().add(order);
		if (last().orders.size()==1) {
			removeLast();
		}
	}

	@Override
	protected void beforePropagate(int[][] order) {
		evs.add(new PropagateEvent());
		last().add(order);
	}

	@Override
	protected void iterPropagate(int[][] order) {
		last().add(order);
	}

	@Override
	protected void afterPropagate(int[][] order) {
		if (last().orders.size()==1) {
			removeLast();
		}
	}

	private void paintOrder(Graphics2D g, int x, int y, int gs, int pg, IntFunction<int[]> fn) {
		for (int i=0; i<gs; i++) {
			int[] vs = fn.apply(i);
			for (int j=0; j<vs.length; j++) {
				g.setColor(orderColor(odi[vs[j]]/(ln-1f), pg==i, (i&1)==0));
				g.fillRect(x,y,1,1);
				y++;
			}
		}
	}
	
	private Color edgeColor(int a, int b, boolean neg) {
		float f = (gsi[a]&1)==0 && (gsi[b]&1)==0 ? 0 : (gsi[a]&1)==1 && (gsi[b]&1)==1 ? 0.2f : 0.1f;
		if (neg) {
			f = 1-f;
		}
		return new Color(f,f,f);
	}
	
	private Color orderColor(float f, boolean pv, boolean oe) {
		f *= 0.9f;
		float f2 = 1-0.1f*f;
		return pv ? new Color(0,1f,0) : oe ? new Color(f2,f,0) : new Color(0,f,f2);
	}

	public BufferedImage asImage() {
		int size = 0;
		for (Event e : evs) {
			size += e.size();
		}
		int w = 2*ln+evs.size()+size+5;
		int h = ln+2;
		BufferedImage ret = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = ret.createGraphics();
		g.setColor(Color.WHITE);
		g.fillRect(0, 0, w, h);
		
		for (int i=0; i<ln; i++) {
			for (int j=0; j<ln; j++) {
				if (i==j) {
					continue;
				}
				if (fg.applyAsInt(i, j)>0) {
					g.setColor(Color.BLACK);
					g.fillRect(i, j, 1, 1);
				}
				g.setColor(edgeColor(od[i], od[j], fg.applyAsInt(od[i], od[j])<0));
				g.fillRect(w-ln-2+i, j, 1, 1);
			}
		}
		paintOrder(g, ln+1, 0, 1, -1, (int p) -> RJ.populate(ln, (int q) -> q));
		paintOrder(g, w-1, 0, ln, -1, (int p) -> new int[] { od[p] });
		g.setColor(Color.DARK_GRAY);
		g.fillRect(0, h-1, ln+2, 1);
		g.fillRect(w-ln-2, h-1, ln+2, 1);
		
		int x = ln+3;
		for (Event e : evs) {
			int pv = e instanceof PivotEvent ? ((PivotEvent) e).group : -1;
			for (int[][] od : e.orders) {
				paintOrder(g, x, 0, od.length, pv, (int p) -> od[p]);
				g.setColor(e instanceof PivotEvent ? Color.BLUE : e instanceof PropagateEvent ? Color.GREEN : Color.RED);
				g.fillRect(x, h-1, 1, 1);
				x++;
				if (pv>=0 && ((PivotEvent) e).evp!=null) {
					int y=0;
					for (int i=0; i<pv; i++) {
						y+=od[i].length;
					}
					for (int[][] pd : ((PivotEvent) e).evp.orders) {
						paintOrder(g, x, y, pd.length, -1, (int p) -> pd[p]);
						g.setColor(Color.BLUE);
						g.fillRect(x, h-1, 1, 1);
						x++;
					}
				}
				pv = -1;
			}
			x++;
		}
		
		
		

		return ret;
	}
	

	public static abstract class Event {
		
		List<int[][]> orders = new ArrayList<>();

		@Override
		public String toString() {
			StringBuilder ret = new StringBuilder();
			for (int[][] ev : orders) {
				ret.append(Arrays.deepToString(ev)).append('\n');
			}
			return ret.toString();
		}
		
		public void add(int[][] od) {
			if (orders.size()==0 || !Arrays.deepEquals(od, orders.get(orders.size()-1))) {
				orders.add(od);
			}
		}
		
		public int size() {
			return orders.size();
		}
		
	}
	
	public static class PropagateEvent extends Event {
		
		@Override
		public String toString() {
			return "PROPAGATE\n" + super.toString();
		}
		
	}
	
	public static class OrderingEvent extends Event {

		@Override
		public String toString() {
			return "ORDER\n" + super.toString();
		}
		
	}
	
	public static class PivotEvent extends Event {
		
		private final int group;
		private PropagateEvent evp;
		
		public PivotEvent(int group) {
			super();
			this.group = group;
		}
		
		@Override
		public String toString() {
			return "PIVOT "+group+"\n"+super.toString()+(evp==null ? "" : "\n--> "+evp);
		}
		
		@Override
		public int size() {
			return super.size()+(evp==null ? 0 : evp.size());
		}
	}

	public static BufferedImage wrap(int scale, int margin, BufferedImage... imgs) {
		int iw = imgs[0].getWidth();
		int ih = imgs[0].getHeight();
		int w = iw*scale+2*margin;
		int h = imgs.length*ih*scale+(1+imgs.length)*margin;
		BufferedImage ret = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = ret.createGraphics();
		g.setColor(Color.WHITE);
		g.fillRect(0, 0, w, h);
		for (int i=0; i<imgs.length; i++) {
			g.drawImage(imgs[i], margin, margin + i*(margin+ih*scale), iw*scale, ih*scale, null);
		}
		return ret;
	}
		
	public static void run(String outfile, int pms, int ln, BigInteger v) throws IOException {
		IntBinaryOperator g = RJ.asOperator(RJ.decodeUndirected(v));
		
		int[][][] rs = new int[pms][][];
		
		rs[0] = RJ.populate(ln, ln, g);
		Random rnd = new Random(0);
		for (int i=1; i<pms; i++) {
			IntUnaryOperator pm = RJ.randomOrder(ln, rnd);
			rs[i] = RJ.populate(ln, ln, (int a, int b) -> g.applyAsInt(pm.applyAsInt(a), pm.applyAsInt(b)));
		}
		
		BufferedImage[] rts = new BufferedImage[pms];
		for (int i=0; i<pms; i++) {
			int _i = i;
			OrderingVisualizerMain rt = new OrderingVisualizerMain(ln, (int a, int b) -> rs[_i][a][b]);		
			System.out.println(rt);
			rts[i] = rt.asImage();
		}
		ImageIO.write(wrap(4,4,rts), "png", new FileOutputStream(outfile));
	}

	public static void main(String[] args) throws Exception {
		run(args[0], Integer.parseInt(args[1]), Integer.parseInt(args[2]), new BigInteger(args[3]));
	}
	
}
