/*
 * This file is part of Spout.
 *
 * Copyright (c) 2011-2012, SpoutDev <http://www.spout.org/>
 * Spout is licensed under the SpoutDev License Version 1.
 *
 * Spout is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * In addition, 180 days after any changes are published, you can use the
 * software, incorporating those changes, under the terms of the MIT license,
 * as described in the SpoutDev License Version 1.
 *
 * Spout is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License,
 * the MIT license and the SpoutDev License Version 1 along with this program.
 * If not, see <http://www.gnu.org/licenses/> for the GNU Lesser General Public
 * License and see <http://www.spout.org/SpoutDevLicenseV1.txt> for the full license,
 * including the MIT license.
 */
package org.spout.engine.mesh;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.lwjgl.opengl.GL11;
import org.spout.api.model.MeshFace;
import org.spout.api.model.Vertex;
import org.spout.api.render.RenderMaterial;
import org.spout.api.render.Renderer;
import org.spout.api.resource.Resource;
import org.spout.engine.renderer.BatchVertexRenderer;

public class ComposedMesh extends Resource {
	Map<RenderMaterial, ArrayList<MeshFace>> opaqueFacesPerMaterials;
	Map<RenderMaterial, ArrayList<MeshFace>> tranparentFacesPerMaterials;

	List<RenderMaterial> dirtyOpaque;
	List<RenderMaterial> dirtyTransparent;
	//ArrayList<RenderEffect> effects = new ArrayList<RenderEffect>();

	Map<RenderMaterial, Renderer> renderersOpaque;
	Map<RenderMaterial, Renderer> renderersTranparent;

	public ComposedMesh(){
		opaqueFacesPerMaterials = new HashMap<RenderMaterial, ArrayList<MeshFace>>();
		tranparentFacesPerMaterials = new HashMap<RenderMaterial, ArrayList<MeshFace>>();

		dirtyOpaque = new ArrayList<RenderMaterial>();
		dirtyTransparent = new ArrayList<RenderMaterial>();

		renderersOpaque = new HashMap<RenderMaterial, Renderer>();
		renderersTranparent = new HashMap<RenderMaterial, Renderer>();
	}

	private void preBatch(Renderer batcher) {
		/*for (RenderEffect effect : effects) {
			effect.preBatch(batcher);
		}*/
	}

	private void postBatch(Renderer batcher) {
		/*for (RenderEffect effect : effects) {
			effect.postBatch(batcher);
		}*/
	}

	protected void batch(Renderer batcher,RenderMaterial renderMaterial) {
		ArrayList<MeshFace> faces;
		//if(renderMaterial.isOpaque()){
		faces = opaqueFacesPerMaterials.get(renderMaterial);
		/*}else{
		faces = tranparentFacesPerMaterials.get(renderMaterial);
		}*/
		
		if(faces != null){
			for (MeshFace face : faces) {
				for(Vertex vert : face){
					batcher.addTexCoord(vert.texCoord0);
					batcher.addNormal(vert.normal);
					batcher.addVertex(vert.position);
					batcher.addColor(vert.color);
				}
			}
		}
	}

	private void preRender(Renderer batcher) {
		/*for (RenderEffect effect : effects) {
			effect.preDraw(batcher);
		}*/
	}

	private void postRender(Renderer batcher) {
		/*for (RenderEffect effect : effects) {
			effect.postDraw(batcher);
		}*/
	}

	public void batch(){

		for(RenderMaterial material : opaqueFacesPerMaterials.keySet()){
			Renderer renderer = renderersOpaque.get(material);
			if(renderer == null) renderer = BatchVertexRenderer.constructNewBatch(GL11.GL_TRIANGLES);
			preBatch(renderer);
			this.batch(renderer, material);
			postBatch(renderer);
		}

		for(RenderMaterial material : tranparentFacesPerMaterials.keySet()){
			Renderer renderer = renderersTranparent.get(material);
			if(renderer == null) renderer = BatchVertexRenderer.constructNewBatch(GL11.GL_TRIANGLES);
			preBatch(renderer);
			this.batch(renderer, material);
			postBatch(renderer);
		}
	}

	public void render(RenderMaterial material){
		Renderer renderer = null;

		//if(renderMaterial.isOpaque()){
		renderer = renderersOpaque.get(material);
		/*}else{
		renderer = renderersTranparent.get(material);
		}*/

		if(renderer == null) throw new IllegalStateException("Cannot render without batching first!");

		preRender(renderer);
		renderer.render(material);
		postRender(renderer);	
	}

	public Map<RenderMaterial, Renderer> getOpaqueRenderer(){
		return renderersOpaque;
	}

	public Map<RenderMaterial, Renderer> getTransparentRenderer(){
		return renderersTranparent;
	}

	public Map<RenderMaterial, ArrayList<MeshFace>> getOpaqueMesh() {
		return opaqueFacesPerMaterials;
	}

	public Map<RenderMaterial, ArrayList<MeshFace>> getTransparentMesh() {
		return tranparentFacesPerMaterials;
	}
}
