
# Akka Cluster Kubernetes Simulator

A demonstration of a basic Akka Cluster running on Kubernetes traffic simulator for companion visualizer.

This simulator microservice works in conjunction with the
[visualizer](https://github.com/mckeeh3/akka-cluster-kubernetes-visualizer) microservice. The simulator service
sends HTTP traffic to the visualizer service. You can increase or decrease the level of traffic by scaling up
and down the number of Akka Clusters node in the simulator service.

## Installation

[Minikube](https://github.com/mckeeh3/akka-cluster-kubernetes-simulator/blob/main/README-minikube.md)

[Red Hat Code Ready Containers](https://github.com/mckeeh3/akka-cluster-kubernetes-simulator/blob/main/README-minikube.md)

[Amazon EKS](https://github.com/mckeeh3/akka-cluster-kubernetes-simulator/blob/main/README-amazon-eks.md)
