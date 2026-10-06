## TEST CASES FOR SERVER NODES

- [ ] Servernodes that leaves the node network because of connection issues should be able to join again
- [x] New nodes should be spawned even if a follower dies if the node count is less than 3
- [ ] When a new leader is elected, all followers should tell the new leader what quizzes they have so the new leader can retrieve quizzes
- [ ] If a certain quiz is replicated on to few nodes, the leader should replicate them to new nodes
- [ ] Possibly change how the calculation of capacity is made, to make sure that there is enough room on all other nodes if one dies and its quizzes must be replicated?
