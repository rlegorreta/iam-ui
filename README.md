# <img height="25" src="./images/AILLogoSmall.png" width="40"/> IAM-UI

<a href="https://www.legosoft.com.mx"><img height="150px" src="./images/Icon.png" alt="AI Legorreta" align="left"/></a>
Microservice that acts as the user interface for all administration operations for a 
generic IAM (Identity Access Management).

The `iam-service` is the microservice that acts as the back-end for this UI. Is utilizes many initiail concepts for 
`Vaadin-flow` framework (latest version 24.1). From the initial beginner
knowledge of Vaadin (i.e., factad view) to advance concepts as include `d3-js` Javascript
graph framework.

note: this microservice is one of the oldest one, so it shows many `Vaadin-flow` concepts.

# UI for the IAM 

## Introduction

This is a vaadin 24.1 flow application that shows as POCs all UI for IAM administration.
The user can add, delete and assign permits, roles and profiles.

It also has all administrative operations for multi-company security.

It is a complete UI that utilize the `iam-service` as its back end repository. 
Nevertheless all WebClient calls are done using the Spring API gateway.

## Tech concepts

This user interface use many UI concepts as POC in order to demonstrate different forms
to develope a vaadin view:

- Facultad view: the basic concept to us `Vaadin-flow` with java an vaadin API.
- Roles view: include an editor in `kotlin` language.
- Profiles view: Utilize Vok (Vaadin on Kotlin) framework.
- The three views utilize `pagination` useing `data providers`.
- Use `d3.js` to display graphs. Other libraries are used in the bup-ui like 'mv graph`
they are simpler concepts as the `d3.js`.
- Other `d3.js` as the profile view query.
- Edit properties for the `d3.js` like user editor.
- Drag&drop `D&d` with grid component.
- `Vaadin-flow` with Spring Authorization Server i.e., remote Oauth2 log-in). The log-in
process asks for a token to the Spring Authorization Server (oauth 2.0) to get the 
login page and the JWT token.
- `Audit` use the audit operations using the `Kafka` messages.

## Kubernetes container

### Vaadin kubernetes kit

To use Vaadin in `kubernetes` it is best to use Vaadin add-on for `kubernetes`. For more
information see: https://vaadin.com/docs/latest/tools/kubernetes

note: the Kubernetes kit is commercial add on. If we do not want to use it, but still want
to deploy in Kubernetes see next section. The main objective of the Kubernetes kit is for
scaling up or down the UI and have high availability.

The Kubernetes Kit uses a combination of sticky sessions and session replication to 
enable scaling up or down, and high availability. Under normal circumstances, sticky 
sessions are used to route the same user always to the same node or pod. 

Kubernetes Kit is used to deploy Vaadin Flow applications on-premise or in the cloud
using Kubernetes. It helps in creating applications that are scalable, highly available,
and user-friendly. To elaborate, it enables the following:

- Horizontal scalability, saving on cloud costs by allowing applications to scale down
without impacting active user sessions and scale up when needed to meet your user and 
server needs.
- High availability, enabling users to continue their active sessions and keep using
your application even if a server fails.
- Non-disruptive rolling updates that don’t interfere with user sessions, thus reducing
the cost and inconvenience of after-hour deployments.

Serialization helpers that make it faster and easier to leverage fully horizontal scaling and fail-over.

Because Kubernetes in Vaadin is just for commercial license only, for the AI Legorreta
marketplace that it is 100% open-source it is not implemented for `Vaadin kubernetes kit` 
what is implemented is to be added inside the `kubernetes docker minicube` container. See
https://vaadin.com/blog/deploying-a-vaadin-app-to-kubernetes for more information.

### Vaadin kubernetes inside a minikube

It is assumed that you have the Kubernetes cluster from Docker Desktop running.

First build the Docker image for your application. You then need to make the Docker 
image available to you cluster. With Docker Desktop Kubernetes, this happens automatically. 
With Minikube, you can run `eval $(minikube docker-env)` and then build the image to 
make it available. 

The file `kubernetes.yaml` sets up a deployment with 2 pods (server instances) and a 
load balancer service. You can deploy the application on a Kubernetes cluster using:

```
kubectl apply -f kubernetes.yaml
```

If everything works, you can access your application by opening http://localhost:8190/.

note: Since this application is a User Interface application NO `gate-way` is used.

The load balancer port is defined in `kubernetes.yaml`.

Tip: If you want to understand which pod your requests go to, you can add the value
of `VaadinServletRequest.getCurrent().getLocalAddr()` somewhere in your UI.

#### Troubleshooting

If something is not working, you can try one of the following commands to see what is
deployed and their status.

```
kubectl get pods
kubectl get services
kubectl get deployments
```

If the pods say `Container image "iam-ui:latest" is not present with pull policy of Never` 
then you have not built your application using Docker or there is a mismatch in the
name. Use `docker images ls` to see which images are available.

If you need even more information, you can run

```
kubectl cluster-info dump
```

that will probably give you too much information but might reveal the cause of a problem.

If you want to remove your whole deployment and start over, run

```
kubectl delete -f kubernetes.yaml
```

## Docker container

### Compilation for local environment

This projects utilize `gradle` and no `maven`. For more information how to create a Vaadin application
with `gradle` see: https://vaadin.com/docs/latest/guide/start/gradle

### Running locally in development mode

If we want to run the IAM UI outside docker the process is as always. Inside the IntelliJ IDEA just run
the project and from the terminal. 

Running in development mode:

```bash
./gradlew clean
./gradlew clean bootRun
```
Run the following command in this repo, to create necessary Vaadin config files:
```
./gradlew clean vaadinPrepareFrontend
```
The build/vaadin-generated/ folder will now contain proper configuration files.

### Running locally in production mode

Vaadin needs to run in "Production mode" inside the docker and not in "Develop mode". For
more information of Vaadin modes see:

https://vaadin.com/docs/v14/flow/production/tutorial-production-mode-basic.html

To run it in production mode execute the following:

```
./gradlew clean
./gradlew build -Pvaadin.productionMode
```

That will build this app in production mode as a runnable jar archive; please find the jar file in
`build/libs/iam-ui*.jar`. You can run the JAR file inside IntelliJ or with:

```
cd build/libs/
java -jar iam-ui*.jar
```

Wait for the application to start

Open http://localhost:8190/ to view the login panel for the  application.

### Load Balanced

Load balanced is just needed for `docker-compose` only because for `kubernetes` it native
support. To enabled load balance the property must be set to:

``iam-ui.kubernetes = false``

The back-end microservice `iam-service` for `docker-compose` container more than one 
instances are created. For information about load balanced see the article:

https://www.linkedin.com/pulse/client-side-load-balance-spring-boot-microservice-docker-rodrigues/?published=t

The code is in:

https://github.com/rs-renato/service-disvovery


Following the guidance for this article we have:

- Eureka for the service discovery. Just for `docker-compose`.
- Gateway operates like a front UI, so is reads the service registry and does not net a health check.
- The microservice `iam-service` is scaled to more than once for demo purpose, so three
containers are created: iam-service-1, ima-service-2 & iam-service3.
- The `@Loadbalanced` is used in all services that use `iam-service` (i.e, web client instance)
- When a service is down (e.g., iam-service1) we use Hystrics to catch the error in 
FacultadService as demo.

### Run it inside the docker desktop

After the compilation in production mode was executed correctly build the docker image:

```bash
./gradlew bootBuildImage
```

Then change to the docker-compose directory

```bash
cd docker-platform-ui
docker-compose up --rm
```

And to run the UI inside the docker:

```bash
docker-compose -f ./target/docker/docker-platform-ui/docker-compose.yml up -d
```

Wait for the application to start

The application can be run inside the Docker desktop dashboard (recommended one).

Open http://localhost:8190/ to view the login panel for the  application.

## How to install Fusion lumo CSS Layouts

```bash
npm i lumo-css-framework
```

For more use of `Vaadin 24.1` this step is not necessary.

## Chrome activate service worker for domains different from local host

user this link: chrome://flags/#unsafely-treat-insecure-origin-as-secure

## Useful links

- Read the documentation at [vaadin.com/docs](https://vaadin.com/docs).
- Follow the tutorial at [vaadin.com/docs/latest/tutorial/overview](https://vaadin.com/docs/latest/tutorial/overview).
- Create new projects at [start.vaadin.com](https://start.vaadin.com/).
- Search UI components and their usage examples at [vaadin.com/docs/latest/components](https://vaadin.com/docs/latest/components).
- View use case applications that demonstrate Vaadin capabilities at [vaadin.com/examples-and-demos](https://vaadin.com/examples-and-demos).
- Build any UI without custom CSS by discovering Vaadin's set of [CSS utility classes](https://vaadin.com/docs/styling/lumo/utility-classes).
- Find a collection of solutions to common use cases at [cookbook.vaadin.com](https://cookbook.vaadin.com/).
- Find add-ons at [vaadin.com/directory](https://vaadin.com/directory).
- Ask questions on [Stack Overflow](https://stackoverflow.com/questions/tagged/vaadin) or join our [Discord channel](https://discord.gg/MYFq5RTbBn).
- Report issues, create pull requests in [GitHub](https://github.com/vaadin).


### Contact AI Legorreta

Feel free to reach out to AI Legorreta on [web page](https://legosoft.com.mx).


Version: 2.0.0
©LegoSoft Soluciones, S.C., 2023

