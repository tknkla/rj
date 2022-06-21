Programming Contract
====================

This document explains the programming contract which all code of RJ abide.

## Coding style

Indentation is done with tabs. While there is no absolute limit of the maximum line length the recommended length is somewhere within 100-200 characters. The main priority is readability and logical presentation of the code.

## Semantic versioning

All classes and methods that are part of the API are marked with *since*-tag and abide the principles of [semantic versioning](https://semver.org). All methods not marked this way may be changed or removed in future releases.

## Performance is a priority

High performance is the main priority after proper functionality. No algorithms in RJ must ever do anything "just in case."

## Permutation as an argument

All algoritms take the permutation group of the input as an argument. The permutation group may be expressed in whatever form is approriate - for example as a truth table (SetOperator), a comparator lambda or a group operator.

## Assumption of immutability

All algorithms assume that all inputs are immutable and therefore trivially thread-safe. Algorithms may also return all or parts of the original input as (part of) the output. If mutability of the inputs is required, the relevant objects need to be duplicated manually to avoid unstable behaviour.

## Lambdas over arrays

Most of the arguments for algorithms are provided as lambdas or other similar interfaces. Arrays are generally used only in cases where using lambdas would cause unnecessary inconvenience.

## Arrays over collections

No specific support is provided for Java collections (java.util) as all the necessary functionality can be trivially implemented with lambdas.

## Method overloading

Naming convention of methods uses method overloading extensively; all methods which perform same or similar function have same or similar name.

## Supported types

Most methods support object (by generics) and primitive int and long. Some methods support only int as array indices in Java are ints. No direct support is *currently* provided for floating point types as algoritms for computation of relative order are very sensitive to floating point rounding errors and/or proper associativity and hence unstable if not outright unusable with floats. In other words, some redesign is required.