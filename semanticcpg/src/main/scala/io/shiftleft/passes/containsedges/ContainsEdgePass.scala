package io.shiftleft.passes.containsedges

import gremlin.scala._
import io.shiftleft.codepropertygraph.generated.nodes.StoredNode
import io.shiftleft.codepropertygraph.generated.{EdgeTypes, NodeTypes}
import io.shiftleft.passes.{CpgPass, DiffGraph, ParallelIteratorExecutor}
import io.shiftleft.queryprimitives.steps.GremlinScalaIterator
import io.shiftleft.queryprimitives.utils.ExpandTo

/**
  * This pass has MethodStubCreator and TypeDeclStubCreator as prerequisite for
  * language frontends which do not provide method stubs and type decl stubs.
  */
class ContainsEdgePass(graph: ScalaGraph) extends CpgPass(graph) {

  private val sourceTypes = List(
    NodeTypes.METHOD,
    NodeTypes.TYPE_DECL,
    NodeTypes.FILE
  )

  private val destinationTypes = List(
    NodeTypes.BLOCK,
    NodeTypes.IDENTIFIER,
    NodeTypes.RETURN,
    NodeTypes.METHOD,
    NodeTypes.TYPE_DECL,
    NodeTypes.CALL,
    NodeTypes.LITERAL,
    NodeTypes.METHOD_REF,
    NodeTypes.UNKNOWN
  )

  override def run(): Iterator[DiffGraph] = {
    val dstGraph = new DiffGraph

    val sourceVerticesIterator = GremlinScalaIterator(graph.V.hasLabel(sourceTypes.head, sourceTypes.tail: _*))

    new ParallelIteratorExecutor(sourceVerticesIterator).map(perSource)
  }

  private def perSource(source: Vertex): DiffGraph = {
    val dstGraph = new DiffGraph()

    ExpandTo
      .walkAST(
        source.start
          .out(EdgeTypes.AST)
          .until(v => v.hasLabel(sourceTypes.head, sourceTypes.tail: _*)))
      .sideEffect(destination =>
        if (destinationTypes.contains(destination.label())) {
          dstGraph.addEdgeInOriginal(source.asInstanceOf[StoredNode],
                                     destination.asInstanceOf[StoredNode],
                                     EdgeTypes.CONTAINS)
      })
      .iterate()

    dstGraph
  }
}
