## TEST CASES FOR SERVER NODES

- [X] Servernodes that leaves the node network because of connection issues should be able to join again
- [x] New nodes should be spawned even if a follower dies if the node count is less than 3
- [X] When a new leader is elected, all followers should tell the new leader what quizzes they have so the new leader can retrieve quizzes
