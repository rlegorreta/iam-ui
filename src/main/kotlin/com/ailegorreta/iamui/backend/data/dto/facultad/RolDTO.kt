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
 *  RolDTO.kt
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
 * Data class for RolDTO. 
 *
 * @author rlh
 * @project : iam-ui
 * @date September 2023
 */
@JsonIgnoreProperties(value = ["activoStr", "dndDropTitle", "graph"])
data class RolDTO @JvmOverloads constructor(override var id : Long? = null,
					@field:NotNull(message = "campo requerido")
					@field:Min(1, message = "El identificador del rol tiene que ser mayor a uno")
					var idRol: Long,
					@field:NotNull(message = "campo requerido")
					@field:Size(min = 4, max = 30, message = "El nombre del rol debe ser de 4 a 30 caracteres")
					var nombre: String,
					@JsonProperty("activo")
					var activo: Boolean = true,
					var usuarioModificacion: String,
					var fechaModificacion: LocalDateTime = LocalDateTime.now(),
					var facultades: ArrayList<FacultadDTO> = ArrayList()): GraphDTO, DnDDTO {
		
	companion object {
		val dndDropTitle = "Arrastar para asignar a:"
	}
	
	var graph: GraphRolFacultad? = null;
	
	fun getActivoStr() = if (activo) "Activo" else "Suspendido"
	
	fun setActivoStr(activoStr: String):Unit {
		activo = activoStr.equals("Activo")
	}
	
	override fun addNode(mapper: ObjectMapper, node: DnDDTO): String {
		if (node is FacultadDTO)
			facultades.add(node)
		
		return jsonString(mapper)
	}
	
	override fun deleteNode(node: DnDDTO): Unit {
		if (node is FacultadDTO)
			facultades.remove(node)		
	}
	
	fun facultadById(id : Long): FacultadDTO? {
		facultades.forEach{ if (it.id == id) return it}
		
		return null
	}

	override val dndDropTitle: String get() = Companion.dndDropTitle + nombre
		
	override fun jsonString(mapper: ObjectMapper): String {
		graph = GraphRolFacultad.mapFromEntity(this)

		return mapper.writeValueAsString(graph)
	}
	
	override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RolDTO) return false

		return nombre == other.nombre
	}

	override fun hashCode(): Int = id?.hashCode() ?: 0
}

/**
 * Data class to generate roles and permits graph.
 *
 * This is done in the Client since there is no IAM-server-repo server
 * that generates this graph.
 *
 */
data class GraphRolFacultad(val nodes: List<Node>,
							val edges: List<Link>) {
		
    companion object {
		fun mapFromEntity(rol : RolDTO) : GraphRolFacultad {
            val nodes = ArrayList<Node>()
            val edges = ArrayList<Link>()
			var nodeId = 1;
			val rolNodeId = nodeId					
			
			val nodeRol = Node(id = nodeId, idNeo4j = if (rol.id != null) rol.id!! else -1,
                    caption = rol.nombre,
                    type = "rol",
                    subType = rol.activo)
			
			nodes.add(nodeRol)
			nodeId++
			if (rol.facultades.isEmpty())
				return GraphRolFacultad(nodes, edges)
			
            rol.facultades.forEach {
					val nodeFacultad = Node(id = nodeId, idNeo4j = it.id!!, caption = it.nombre,
                            subType = it.activo, type = "facultad")
				
            		nodes.add(nodeFacultad)
            		edges.add(Link(source = rolNodeId, target = nodeId, caption = "permiso"))
				    nodeId++
            	}
					
            return GraphRolFacultad(nodes, edges)
        }
    }
	
	fun node(nodeId: Int): Node? {
		nodes.forEach { if (it.id == nodeId) return it }
		
		return null
	}
}
