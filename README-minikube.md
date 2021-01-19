
# Deployment to Kubernetes Minikube

Instructions for deploying to Kubernetes Minikube.

## Install and Set Up kubectl

Follow the [instructions](https://kubernetes.io/docs/tasks/tools/install-kubectl/) for installing the `kubectl` CLI.

The `kubectl` CLI provides a nice Kubectl Autocomplete feature for `bash` and `zsh`.
See the [kubectl Cheat Sheet](https://kubernetes.io/docs/reference/kubectl/cheatsheet/#kubectl-autocomplete) for instructions.

### Install Minikube

Follow the [instructions](https://kubernetes.io/docs/tasks/tools/install-minikube/) for installing Minikube.

### Optional Kubernetes Utilities

Also, consider installing [kubectx](https://github.com/ahmetb/kubectx), which also includes `kubens`.
Mac:

~~~bash
brew install kubectx
~~~

Arch Linux:

~~~bash
yay kubectx
~~~

## Start Minikube

You may want to allocate more CPU and memory capacity to run the WoW application than the defaults. There are two `minikube` command options available for adjusting the CPU and memory allocation settings.

~~~bash
minikube start --driver=virtualbox --cpus=C --memory=M
~~~

For example, allocate 4 CPUs and 10 gig of memory.

~~~bash
minikube start --driver=virtualbox --cpus=4 --memory=10g
~~~

### Clone the GitHub Repo

Git clone the project repoisitory.

~~~bash
git clone https://github.com/mckeeh3/akka-cluster-kubernetes-simulator.git
~~~

### Build and Deploy to MiniKube

From the `akka-cluster-kubernetes-simulator` project directory.

Before the build, set up the Docker environment variables using the following commands.

~~~bash
$ minikube docker-env

export DOCKER_TLS_VERIFY="1"
export DOCKER_HOST="tcp://192.168.99.102:2376"
export DOCKER_CERT_PATH="/home/hxmc/.minikube/certs"
export MINIKUBE_ACTIVE_DOCKERD="minikube"

# To point your shell to minikube's docker-daemon, run:
# eval $(minikube -p minikube docker-env)
~~~

Copy and paster the above `eval` command.

~~~bash
eval $(minikube -p minikube docker-env)
~~~

Build the project, which will create a new Docker image.

~~~bash
mvn clean package docker:build
~~~

Create the Kubernetes namespace. The namespace only needs to be created once.

~~~bash
kubectl create namespace simulator
~~~

Set this namespace as the default for subsequent `kubectl` commands.

~~~bash
$ kubectl config set-context --current --namespace=simulator

Context "minikube" modified.
~~~

Deploy the Docker images to the Kubernetes cluster.

~~~bash
$ kubectl apply -f kubernetes/akka-cluster-minikube.yml

deployment.apps/simulator created
role.rbac.authorization.k8s.io/pod-reader created
rolebinding.rbac.authorization.k8s.io/read-pods created
~~~

Check if the pods are running. This may take a few moments.

~~~bash
$ kubectl get pods

NAME                         READY   STATUS    RESTARTS   AGE
simulator-5b4cf87d4d-5b7w6   1/1     Running   0          3m31s
simulator-5b4cf87d4d-hzp7n   1/1     Running   0          3m31s
simulator-5b4cf87d4d-pbst2   1/1     Running   0          3m31s
~~~
