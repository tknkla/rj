RJ - Relative (Algorithms and Related Utilities for) Java
=========================================================

**RJ** is a collection of parallelized elementary algorithms and related utilities suitable for writing relative algorithms in Java (1.8+). RJ includes the reference implementation of an algorithm which can be used for computing a *relative order*.

**Relative order** is a tractable (less than n² in the best case, approximately n⁴ in the worst case) computational problem into which (undirected) [graph canonization](https://en.wikipedia.org/wiki/Graph_canonization) and [graph isomorphism](https://en.wikipedia.org/wiki/Graph_isomorphism) can be reduced in polynomial time.

In practive, relative order is a (pre)order, computation of which requires no transitivity between the elements to be ordered. It can be used to compute canonical forms for arbitrary data structures and solving complex isomorphism problems.

**Relative algorithms** are algorithms which make no assumptions regarding symmetric choices. Such algorithms are significantly faster than algorithms which make such assumptions in the case of problems where the amount of symmetry depends on the size of the input.

For a scientific article which explains the theory behind relative order, how it is computed in polynomial time and what relative algorithms generally are, see:

- <https://tknkla.com/l/relative_order>
 
RJ is written by *Timo Santasalo* and is provided as free software under [MIT](https://mit-license.org) license by [TKNKLA](https://tknkla.com).

## Changelog

- (22.6.2022) **1.0.0**: The initial release.

## Documentation

For a complete and detailed documentation, see javadoc:

- <https://rj.tknkla.net/javadoc/1.0.0/>

## Building and installing

As RJ is a programming library the specific installation instructions depend on the build system being used.

RJ is available as a Github package so no explicit downloading, building or installation is necessary. In short, use the following dependency information:

- repository: <https://maven.pkg.github.com/tknkla/rj>
- groupId: *com.tknkla.rj*
- artifactId: *rj*
- version: *1.0.0*

RJ itself is built with [Apache Maven](https://maven.apache.org). See Maven documentation for detailed instructions on building and installing.

RJ has no external dependencies.

## Examples

### Computing a relative order of vertices of an undirected graph

Computing relative order requires four arguments: the initial preorder (a sequence of sets of vertices), the graph, a group operator and an optional vertex comparator. The input for the algorithm is provided as a lambda along with an array of vertices. Vertices may be ints, longs or objects. Using ints or longs is recommended especially in the case of large inputs.

First let us assume that there are n vertices:

	final int n = ...;

To simplify things, let us assume that the vertices are integers from 0 to n. The *populate* method is used to generate arrays from a lambda (in parallel - there is no guarantee of the order in which the array is populated):

	int[] vertices = RJ.populate(n, (p) -> p);

The initial preorder (the assumption of absolute, explained in the paper mentioned above) in this case is the set of vertices wrapped in an array:

	int[] preorder = new int[]{ vertices };

The input graph in this example is a binary predicate over the set of vertices (integers from 0 to n-1). The lambda returns true if there is an edge between the given pair of vertices:
	
	IntBinaryPredicate ugraph = (a, b) -> ...;

RJ requires the graph as a lambda which returns integers (or longs or objects). A binary predicate must thus be wrapped and/or converted to such lambda:

	IntBinaryOperator graph = RJ.asWeighted(g);

If the edge function is computationally heavy, it may be useful to first store the graph into an array instead:

	int[][] edges = RJ.populate(n, n, RJ.asWeighted(g));
	IntBinaryOperator graph = (a, b) -> edges[a][b];

Relative order can be computed with anything that supports group semantics. Let us assume that addition is used:

	IntGroupOperator group = IntGroupOperator.ADDITIVE;

As the vertices are already in their natural order it is not necessary to provide a vertex comparator:

	IntBinaryOperator comp = null;

If provided, the vertices shall be ordered by the comparator function - if not, the presentation order is used (the sorting algorithm is stable). The following produces identical results in the case of this example:

	IntBinaryOperator comp = Integer::compare;

To compute a (truly) relative order of automorphic groups, or a sequence of sets of vertices, use *groups*:

	int[][] gorder = RJ.groups(preorder, graph, group, comp);

To compute a (strongly) relative order of vertices, or a sequence of vertices, use *order*:

	int[] vorder = RJ.order(preorder, graph, group, comp);
	
If both truly and strongly relative orders are needed, the output of *groups* may be provided as input for *order* (this will save some time):

	int[] vorder = RJ.order(gorder, graph, group, comp);

#### Computing the canonical form of an unidirected graph

The output of the previous example may be used to create a canonical representation of the original input:

	IntBinaryPredicate cgraph = (a, b) -> ugraph.test(vorder[a], vorder[b]);

#### Testing for isomorphism between a pair of undirected graphs

Compute canonical forms of the graphs to be tested as in the previous example; isomorphism can then be trivially detected by a simple comparision:

	for (int i=0; i<n; i++) {
		for (int j=0; j<n; j++) {
			if (cgraph1.test(i,j)!=cgraph2.test(i,j)) {
				return false; // graphs are not isomorphic
			}
		}
	}
	return true; // graphs are isomorphic

### More examples

Some example code can be found in the test source directory:

- A minimal command line interface for computing a relative order (the input format is documented in the javadoc): <https://github.com/tknkla/rj/tree/develop/src/test/java/com/tknkla/rj/examples/cli> (in the develop branch)
- An algorithm which counts the number of canonical graphs (a brute force method for determining the relativity class of undirected graph): <https://github.com/tknkla/rj/blob/main/src/test/java/com/tknkla/rj/examples/cgcounter>
- The algorithm used to produce the visualizations (of computation of relative order) in the article mentioned above: <https://github.com/tknkla/rj/blob/main/src/test/java/com/tknkla/rj/examples/visualizer>

The source code of RJ itself also offers several examples: for example the algorithm which computes a relative order utilizes majority of algorithms in class RJ, so following that code provides examples for majority of the functions in RJ.

## Contributing

RJ is mostly feature complete (regarding the original scope) and mostly bug-free (well tested with coverage over 90%). The best way to contribute is thus to use it and if you encounter a bug, let me know. Any functionality that is different from the documented functionality is considered a bug.

However if you feel that there is some algorithm that should be a part of RJ and especially if you're willing to write it, let me know. See CONTRACT.md for details on the programming contract all code of RJ must abide.

### Roadmap

- Support for floats and doubles
- Special cases of merge (set operations): one-to-many, many-to-one and one-to-one
- Set predicate operation (contains all or any; a parallel opportunistic search)
- Improvement of parallel execution (api and performance)
