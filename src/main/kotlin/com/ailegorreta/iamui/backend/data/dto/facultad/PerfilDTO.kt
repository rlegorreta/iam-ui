/* Copyright (c) 2023, LegoSoft Soluciones, S.C.
* All rights reserved.
*
* Redistribution and use in source and binary forms, with or without
* modification, are not permitted.
*
* THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
* AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
* IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
* ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
* LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
* CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
* SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
* INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
* CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
* ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
* POSSIBILITY OF SUCH DAMAGE.
*
*  PerfilDTO.kt
*
 *  Developed 2023 by LegoSoftSoluciones, S.C. www.legosoft.com.mx
*/
package com.ailegorreta.iamui.backend.data.dto.facultad

import com.fasterxml.jackson.annotation.*
import com.fasterxml.jackson.databind.ObjectMapper
import com.ailegorreta.iamui.backend.data.dto.*
import java.time.LocalDateTime

import jakarta.validation.constraints.*

/**
 * Data class for PerfilDTO. 
 *
 * @author rlh
 * @project : iam-ui
 * @date September 2023
 */
@JsonIgnoreProperties(value = arrayOf("activoStr","dndDropTitle","graph"))
data class PerfilDTO @JvmOverloads constructor(var id : Long? = null,
					@field:NotNull(message = "campo requerido")
					@field:Size(min = 4, max = 25, message="El nombre del perfil debe ser de 4 a 25 caracteres")
					var nombre: String,
					var descripcion: String? = null,
					@JsonProperty("activo")
				    var activo: Boolean = true,
					@JsonProperty("patron")
				    var patron: Boolean = false,
					var usuarioModificacion: String,
					var fechaModificacion: LocalDateTime = LocalDateTime.now(),
				    var roles: ArrayList<RolDTO> = ArrayList()): GraphDTO {

	companion object {
		val dndDropTitle = "Arrastar para asignar a:"
	}

	var graph: GraphPerfilRol? = null;
	
	fun getActivoStr() = if (activo) "Activo" else "Suspendido"
	
	fun setActivoStr(activoStr: String):Unit {
		activo = activoStr.equals("Activo")
	}

	override fun addNode(mapper: ObjectMapper, node: DnDDTO): String {
		if (node is RolDTO)
			roles.add(node)
		
		return jsonString(mapper)
	}
	
	override fun deleteNode(node: DnDDTO): Unit {
		if (node is RolDTO)
			roles.remove(node)		
	}

	fun rolById(id : Long): RolDTO? {
		roles.forEach{ if (it.id == id) return it}
		
		return null
	}
	
	override val dndDropTitle: String get() = RolDTO.dndDropTitle + nombre

	override fun jsonString(mapper: ObjectMapper): String {
		graph = GraphPerfilRol.mapFromEntity(this)
		
		return mapper.writeValueAsString(graph)
	}
				
	override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PerfilDTO) return false

		return nombre == other.nombre
	}

	override fun hashCode(): Int = id?.hashCode() ?: 0
}

/*
 * Data class to generate profiles and roles graph.
 *
 * This is done in the Client since there is no IAM-server-repo server
 * that generates this graph.
 *
 */
data class GraphPerfilRol(val nodes: List<Node>,
						  val edges: List<Link>) {
		
    companion object {
				
		fun mapFromEntity(perfil : PerfilDTO) : GraphPerfilRol {
            val nodes = ArrayList<Node>()
            val edges = ArrayList<Link>()
			var nodeId = 1;
			val perfilNodeId = nodeId
			val nodePerfil = Node(id = nodeId, idNeo4j = if (perfil.id != null) perfil.id!! else -1,
                    caption = perfil.nombre,
                    type = "perfil",
                    subType = perfil.activo)
			
			nodes.add(nodePerfil)
			nodeId++
			if (perfil.roles.isEmpty())
				return GraphPerfilRol(nodes, edges)
			
            perfil.roles.forEach {
					val nodeRol = Node(id = nodeId, idNeo4j = it.id!!, caption = it.nombre,
                            subType = it.activo,
                            type = "rol")
				
            		nodes.add(nodeRol)
            		edges.add(Link(source = perfilNodeId, target = nodeId, caption = "rol"))
				    nodeId++
            	}
					
            return GraphPerfilRol(nodes, edges)
        }
    }
	
	fun node(nodeId: Int): Node? {
		nodes.forEach { if (it.id == nodeId) return it }
		
		return null
	}
}

/*
 * Data class to generate a d3.js Tree for complete profile view
 */
data class TreePerfil(val name: String,
					  val parent: String?,
					  val children: List<TreePerfil>? = null) {

  companion object {
	  fun mapFromEntity(perfil: PerfilDTO): TreePerfil {
		  val roles = ArrayList<TreePerfil>()

		  perfil.roles.forEach {
			  val facultades = ArrayList<TreePerfil>()
			  val rolName = it.nombre

			  it.facultades.forEach{
				  facultades.add(TreePerfil(name = it.nombre, parent = rolName))
			  }
			  roles.add(TreePerfil(name = it.nombre, parent = perfil.nombre, children = facultades))
		  }
		  return TreePerfil(name = perfil.nombre, parent = null, children = roles)
	  }
  }
}
