RJ - Relative (Algorithms and Related Utilities for) Java
=========================================================

**See <https://rj.tknkla.net> for more information including complete documentation and examples.**

---

**RJ** is a collection of transparently parallelized elementary algorithms and related utilities suitable for writing *relative algorithms* in Java (1.8+), including the reference implementation of an algorithm for computing a *relative order*.

**Relative algorithms** are algorithms which make no assumptions regarding symmetric choices in contrast to *absolute algorithms* which make such choices. Relative algorithms are faster than absolute algorithms by the worst case complexity and slower by the best case complexity and thus more efficient in the case of problems where the amount of symmetry is relative to the size of the input.

The algorithms in RJ complement those in *java.util.Collections* and *java.util.Arrays* and are intended as building blocks of efficient parallel algorithms handling large amounts of complex data.

**Relative order** is a tractable (less than n² in the best case and approximately n⁴ in the worst case for a graph of n vertices) computational problem into which (undirected) [graph canonization](https://en.wikipedia.org/wiki/Graph_canonization) and [graph isomorphism](https://en.wikipedia.org/wiki/Graph_isomorphism) can be reduced in polynomial time.

In practive, relative order is a (pre)order that is computed solely based on the relations between the elements to be ordered, disregarding their absolute values. In other words it requires no transitivity between the relations of the elements to be ordered. It can be used to compute canonical forms and find complex isomorphisms in arbitrary data structures.

For a scientific article which explains the theory behind relative order, how it is computed in polynomial time and what relative algorithms generally are, see:

- [Relative Order - A Polynomial Time Algorithm for Undirected Graph Canonization](https://tknkla.com/l/relative_order)
 
RJ is written by *Timo Santasalo* and is provided as free software under [MIT](https://mit-license.org) license and maintained by [TKNKLA](https://tknkla.com).

## Building and installing

As RJ is a programming library the specific installation instructions depend on the build system being used.

RJ itself is built with [Apache Maven](https://maven.apache.org). See Maven documentation for detailed instructions on building and installing.

RJ has no external dependencies.

# Contributing

RJ is mostly feature complete (regarding the original scope) and mostly bug-free (well tested with coverage over 90%). The best way to contribute is thus to use it and if you encounter a bug, let me know. Anything that is contrary to the documented (Javadoc) functionality is considered a bug.

However if you feel that there is some algorithm that should be part of RJ and especially if you're willing to write it, let me know. See [CONTRACT](CONTRACT.md) for details on the programming contract all code of RJ must abide.

## Roadmap

- Improvement of parallel execution (api and performance)
- Set predicate operation (contains all or any; a parallel opportunistic search)
- Support for floats and doubles
