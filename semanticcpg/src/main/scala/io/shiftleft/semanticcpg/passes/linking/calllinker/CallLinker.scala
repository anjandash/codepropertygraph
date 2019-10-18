package io.shiftleft.semanticcpg.passes.linking.calllinker

import gremlin.scala._
import io.shiftleft.codepropertygraph.Cpg
import io.shiftleft.codepropertygraph.generated.{DispatchTypes, EdgeTypes, NodeTypes, nodes}
import io.shiftleft.passes.{CpgPass, DiffGraph}
import io.shiftleft.semanticcpg.language._
import io.shiftleft.Implicits._
import org.apache.tinkerpop.gremlin.structure.Direction
import org.apache.logging.log4j.{LogManager, Logger}

import scala.collection.JavaConverters._

class CallLinker(cpg: Cpg) extends CpgPass(cpg) {
  import CallLinker._
  private var methodFullNameToNode = Map.empty[String, nodes.StoredNode]

  /**
    * Main method of enhancement - to be implemented by child class
    **/
  override def run(): Iterator[DiffGraph] = {
    val dstGraph = new DiffGraph()

    cpg.graph.V
      .hasLabel(NodeTypes.METHOD)
      .sideEffectWithTraverser { traverser =>
        val method = traverser.get.asInstanceOf[nodes.Method]
        methodFullNameToNode += (method.fullName -> method)
      }
      .iterate()

    cpg.call.toIterator().foreach { call =>
      try {
        linkCall(call, dstGraph)
      } catch {
        case exception: Exception =>
          throw new RuntimeException(exception)
      }
    }

    Iterator(dstGraph)
  }

  private def linkCall(call: nodes.Call, dstGraph: DiffGraph): Unit = {
    val resolvedMethodOption =
      if (call.dispatchType == DispatchTypes.STATIC_DISPATCH) {
        methodFullNameToNode.get(call.methodFullName)
      } else {
        val receiverIt = call.vertices(Direction.OUT, EdgeTypes.RECEIVER)
        if (receiverIt.hasNext) {
          val receiver = receiverIt.next
          val receiverTypeDecl = receiver
            .vertices(Direction.OUT, EdgeTypes.EVAL_TYPE)
            .nextChecked
            .vertices(Direction.OUT, EdgeTypes.REF)
            .nextChecked

          receiverTypeDecl.vertices(Direction.OUT, EdgeTypes.BINDS).asScala.collectFirst {
            case binding: nodes.Binding if binding.name == call.name && binding.signature == call.signature =>
              binding.vertices(Direction.OUT, EdgeTypes.REF).nextChecked.asInstanceOf[nodes.Method]
          }
        } else {
          logger.warn(s"Missing receiver edge on CALL ${call.code}")
          None
        }
      }

    resolvedMethodOption match {
      case Some(method) =>
        dstGraph.addEdgeInOriginal(call, method, EdgeTypes.CALL)
      case None =>
        logger.warn(
          s"Unable to link CALL with METHOD_FULL_NAME ${call.methodFullName}, NAME ${call.name}, SIGNATURE ${call.signature}")
    }
  }
}

object CallLinker {
  private val logger: Logger = LogManager.getLogger(getClass)
}