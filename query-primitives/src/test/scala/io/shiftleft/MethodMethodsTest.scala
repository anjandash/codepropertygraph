package io.shiftleft

import gremlin.scala._
import io.shiftleft.codepropertygraph.generated.{EdgeTypes, NodeTypes, nodes}
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph
import org.scalatest.{Matchers, WordSpec}
import io.shiftleft.queryprimitives.dsl.Implicits._
import io.shiftleft.queryprimitives.dsl.pipetypes.RealPipe.RealPipe

class MethodMethodsTest extends WordSpec with Matchers {
  "foo" in {
    implicit val graph: ScalaGraph = TinkerGraph
      .open(io.shiftleft.codepropertygraph.generated.nodes.Factories.AllAsJava,
        io.shiftleft.codepropertygraph.generated.edges.Factories.AllAsJava)
      .asScala()

    val method = graph + NodeTypes.METHOD
    val methodInst = graph + NodeTypes.METHOD_INST

    methodInst --- EdgeTypes.REF --> method

    val typeMethod = method.asInstanceOf[nodes.Method]

    typeMethod.methodInstance.toList should contain (methodInst)
    typeMethod.methodInstance.map

    //typeMethod.parameter.map(x => x).impl should contain (methodInst)
    //RealPipe(List(1, 2, 3)).map(_ + 1).foreach(x => println(x))


  }
}