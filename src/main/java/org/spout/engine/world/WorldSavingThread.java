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
package org.spout.engine.world;

import java.io.OutputStream;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.LinkedBlockingQueue;

import org.spout.api.Spout;
import org.spout.api.geo.World;
import org.spout.api.geo.cuboid.ChunkSnapshot.EntityType;
import org.spout.api.geo.cuboid.ChunkSnapshot.ExtraData;
import org.spout.api.geo.cuboid.ChunkSnapshot.SnapshotType;
import org.spout.engine.filesystem.WorldFiles;
import org.spout.engine.world.dynamic.DynamicBlockUpdate;

/**
 * Dedicated thread to IO write operations for world chunks
 */
public class WorldSavingThread extends Thread{
	private static WorldSavingThread instance = null;
	private final LinkedBlockingQueue<Callable<SpoutWorld>> queue = new LinkedBlockingQueue<Callable<SpoutWorld>>();
	public WorldSavingThread() {
		super("World Saving Thread");
	}

	public static void startThread() {
		if (instance == null) {
			instance = new WorldSavingThread();
			instance.start();
		}
	}

	public static void saveChunk(SpoutChunk chunk) {
		ChunkSaveTask task = new ChunkSaveTask(chunk);
		if (instance.isInterrupted() || !instance.isAlive()) {
			Spout.getLogger().warning("Attempt to queue chunks for saving after world thread shutdown");
			task.call();
		} else {
			instance.queue.add(task);
		}
	}

	public static void finish() {
		instance.interrupt();
	}

	public static void staticJoin() {
		try {
			instance.join();
		} catch (InterruptedException ie) {
			Spout.getLogger().info("Main thread interruped while waiting for world save thread to end");
		}
	}

	@Override
	public void run() {
		while (!Thread.interrupted()) {
			Callable<SpoutWorld> task;
			try {
				task = queue.take();
				task.call();
			} catch (InterruptedException ignore) {
				break;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		processRemaining();
	}

	private void processRemaining() {
		int toSave = queue.size();
		int saved = 0;
		int lastTenth = 0;
		Callable<SpoutWorld> task;
		while ((task = queue.poll()) != null) {
			try {
				task.call();
			} catch (Exception e) {
				e.printStackTrace();
			}
			int tenth = ((++saved) * 10) / toSave;
			if (tenth != lastTenth) {
				lastTenth = tenth;
				Spout.getLogger().info("Saved " + tenth + "0% of queued chunks");
			}
		}
		Collection<World> worlds = Spout.getEngine().getWorlds();
		for (World w : worlds) {
			((SpoutWorld) w).getRegionFileManager().stopTimeoutThread();
		}
		for (World w : worlds) {
			((SpoutWorld) w).getRegionFileManager().closeAll();
		}
	}

	private static class ChunkSaveTask implements Callable<SpoutWorld> {
		final SpoutChunkSnapshot snapshot;
		final List<DynamicBlockUpdate> blockUpdates;
		final SpoutChunk chunk;
		ChunkSaveTask(SpoutChunk chunk) {
			this.snapshot = (SpoutChunkSnapshot) chunk.getSnapshot(SnapshotType.BOTH, EntityType.BOTH, ExtraData.DATATABLE);
			this.blockUpdates = chunk.getRegion().getDynamicBlockUpdates(chunk);
			this.chunk = chunk;
		}

		@Override
		public SpoutWorld call() {
			SpoutWorld world = chunk.getWorld();
			OutputStream out = world.getChunkOutputStream(snapshot);
			if (out != null) {
				WorldFiles.saveChunk(chunk.getWorld(), snapshot, blockUpdates, out);
				chunk.saveComplete();
			} else {
				Spout.getLogger().severe("World saving thread unable to open file for chunk " + chunk);
			}
			return world;
		}
	}
}
