CRAAPI
======

Completely Redundant ARDrone API - Java implementation of the ARDrone 2.0 interface intended for use with Matlab (made for school project; not polished nor complete...but it works)

I wrote most of this code prior to discovering the YADrone project (https://github.com/MahatmaX/YADrone), which is a much more complete Java interface for the ARDrone. While I have not used it myself, I strongly encourage you check out that project rather than this one. I went down a strange path with resource control and handling concurrent decisions/commands (currently half-baked in the code). It works, but it's unnecessary. Basically, you may control certain drone behaviors by specifying 'behaviors' that go along with drone states. For example, when in the 'grounded' state, the behavior is 'wait for a takeoff command'. While this sounds nice, the implementation is mostly 'silly'.

That said, I wrote this code as part of a 'final project' for CLPS 1520 at Brown University. I have included some 'stubbed out' Matlab code that uses the API to cause the drone to take off, fly in a simple pattern, and then land. It seemed to me that a Java API was the best way to integrate Matlab and the ARDrone.
