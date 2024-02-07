<p align="center">
  <img src="/docs/lepus-constellation.jpg" height="200px" alt="Lepus icon" />
  <br/>
  <strong>Lepus</strong><br/>
  <i>Purely functional Scala client for RabbitMQ and other AMQP 0.9.1 brokers</i>
</p>

<a href="https://typelevel.org/cats/"><img src="https://typelevel.org/cats/img/cats-badge.svg" height="40px" align="right" alt="Cats friendly" /></a>
[![lepus-client Scala version support](https://index.scala-lang.org/hnaderi/lepus/lepus-client/latest.svg?style=flat-square)](https://index.scala-lang.org/hnaderi/lepus/lepus-client)
 [![javadoc](https://javadoc.io/badge2/dev.hnaderi/lepus-docs_3/javadoc.svg?style=flat-square)](https://javadoc.io/doc/dev.hnaderi/lepus-docs_3) 
<img alt="GitHub Workflow Status" src="https://img.shields.io/github/actions/workflow/status/hnaderi/lepus/ci.yml?style=flat-square">
<img alt="GitHub" src="https://img.shields.io/github/license/hnaderi/lepus?style=flat-square">  
![Typelevel Affiliate Project](https://img.shields.io/badge/typelevel-affiliate%20project-FFB4B5.svg?style=flat-square)
[![Scala Steward badge](https://img.shields.io/badge/Scala_Steward-helping-blue.svg?style=flat-square&logo=data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAA4AAAAQCAMAAAARSr4IAAAAVFBMVEUAAACHjojlOy5NWlrKzcYRKjGFjIbp293YycuLa3pYY2LSqql4f3pCUFTgSjNodYRmcXUsPD/NTTbjRS+2jomhgnzNc223cGvZS0HaSD0XLjbaSjElhIr+AAAAAXRSTlMAQObYZgAAAHlJREFUCNdNyosOwyAIhWHAQS1Vt7a77/3fcxxdmv0xwmckutAR1nkm4ggbyEcg/wWmlGLDAA3oL50xi6fk5ffZ3E2E3QfZDCcCN2YtbEWZt+Drc6u6rlqv7Uk0LdKqqr5rk2UCRXOk0vmQKGfc94nOJyQjouF9H/wCc9gECEYfONoAAAAASUVORK5CYII=)](https://scala-steward.org)

## Use
This library is published for scala 3 on all scala platforms (JVM, JS, Native); 
Add this to your build:

```scala
libraryDependencies += "dev.hnaderi" %% "lepus-client" % "<version from releases>"
```

Then you are ready to use Lepus, for tutorial continue to [documentation site](https://lepus.hnaderi.dev)!  
Also please drop a ⭐ if this project interests you. I need encouragement.

## Why?
If you have ever used [Skunk](https://github.com/tpolecat/skunk) you might agree with me on how much it's fun to use and meanwhile insightful for its users. This project started with this in mind and I hope it achieves its goals.

## I mean, why not use the official java client and use Lepus instead?
I'm not in a position to tell you what you should use, but if you are tired of wrappers over the java client,
or want to use more advanced features of RabbitMQ that are not available in other Scala clients, or if you want a clean purely functional client for RabbitMQ (or other AMQP compliant message brokers) take a look at this library and then assess your requirements. You can see [features](https://lepus.hnaderi.dev/features.html) for some more information.

## Examples

Visit [here](https://lepus.hnaderi.dev/examples) or check [example directory](example/src/main/scala/example) for more examples.

## What in the world is Lepus?
Lepus[^1] (/ˈliːpəs/, colloquially /ˈlɛpəs/) is a constellation lying just south of the celestial equator. Its name is Latin for hare. It is located below—immediately south—of Orion (the hunter), and is sometimes represented as a hare being chased by Orion or by Orion's hunting dogs. 

## Disclaimer
This software is a perpetual beta release, while it might be totally okay to use it in production for most cases, it's worthy to mention that you need to manage your risks carefully!

[^1]: https://en.wikipedia.org/wiki/Lepus_(constellation)

-----

<sub>Logo illustration by Catalina Vásquez
from Los Animales del cielo
</sub>
