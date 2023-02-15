<p align="center">
  <img src="/docs/lepus-constellation.jpg" height="200px" alt="Lepus icon" />
  <br/>
  <strong>Lepus</strong><br/>
  <i>Purely functional Scala client for RabbitMQ and other AMQP 0.9.1 brokers</i>
</p>

<a href="https://typelevel.org/cats/"><img src="https://typelevel.org/cats/img/cats-badge.svg" height="40px" align="right" alt="Cats friendly" /></a>
[![lepus-client Scala version support](https://index.scala-lang.org/hnaderi/lepus/lepus-client/latest.svg?style=flat-square)](https://index.scala-lang.org/hnaderi/lepus/lepus-client)
<img alt="GitHub Workflow Status" src="https://img.shields.io/github/actions/workflow/status/hnaderi/lepus/ci.yml?style=flat-square">
<img alt="GitHub" src="https://img.shields.io/github/license/hnaderi/lepus?style=flat-square">  

## Use
This library is published for scala 3 on all scala platforms (JVM, JS, Native); 
Add this to your build:

```scala
libraryDependencies += "dev.hnaderi" %% "lepus-client" % "<version from releases>"
```

Then you are ready to use Lepus, for tutorial continue to [documentation site](https://lepus.hnaderi.dev)!  

## Why?
If you have ever used [Skunk](https://github.com/tpolecat/skunk) you might agree with me on how much it's fun to use and meanwhile insightful for it's users. This project started with this in mind and I hope it achieves its goals.

## I mean, why not use official java client and use Lepus instead?
I'm not in position to tell you what should you use, but if you are tired of wrappers over the java client,
or want to use more advanced features of RabbitMQ that are not available in other scala clients, or if you want a clean purely functional client for RabbitMQ (or other AMQP compliant message brokers) take a look at this library and then assess your requirements. You can see [features](https://lepus.hnaderi.dev/features.html) for some more informations.

## What in the world is Lepus?
Lepus[^1] (/ˈliːpəs/, colloquially /ˈlɛpəs/) is a constellation lying just south of the celestial equator. Its name is Latin for hare. It is located below—immediately south—of Orion (the hunter), and is sometimes represented as a hare being chased by Orion or by Orion's hunting dogs. 

## Disclaimer
This is a work in progress in alpha release mode, and might not be ready for production use yet, manage your risks carefully!

[^1]: https://en.wikipedia.org/wiki/Lepus_(constellation)

-----

<sub>Logo illustration by Catalina Vásquez
from Los Animales del cielo
</sub>
