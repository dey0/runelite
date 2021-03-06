/*
 * Copyright (c) 2016-2017, Adam <Adam@sigterm.info>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package net.runelite.cache.server;

import com.google.common.primitives.Ints;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import java.io.IOException;
import java.util.Arrays;
import net.runelite.cache.fs.Archive;
import net.runelite.cache.fs.Index;
import net.runelite.cache.fs.Store;
import net.runelite.cache.fs.jagex.CompressionType;
import net.runelite.cache.fs.jagex.DataFile;
import net.runelite.cache.protocol.packets.ArchiveRequestPacket;
import net.runelite.cache.protocol.packets.ArchiveResponsePacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ArchiveRequestHandler extends SimpleChannelInboundHandler<ArchiveRequestPacket>
{
	private static final Logger logger = LoggerFactory.getLogger(ArchiveRequestHandler.class);

	private final Store store;

	public ArchiveRequestHandler(Store store)
	{
		this.store = store;
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, ArchiveRequestPacket archiveRequest) throws Exception
	{
		if (archiveRequest.getIndex() == 255)
		{
			handleRequest255(ctx, archiveRequest.getIndex(),
				archiveRequest.getArchive());
		}
		else
		{
			handleRequest(ctx, archiveRequest.getIndex(),
				archiveRequest.getArchive());
		}
	}

	private void handleRequest255(ChannelHandlerContext ctx, int index, int archiveId) throws IOException
	{
		logger.info("Client {} requests 255: index {}, archive {}", ctx.channel().remoteAddress(), index, archiveId);

		byte[] compressed;
		if (archiveId == 255)
		{
			// index 255 data, for each index:
			// 4 byte crc
			// 4 byte revision
			ByteBuf buffer = Unpooled.buffer(store.getIndexes().size() * 8);
			for (Index i : store.getIndexes())
			{
				buffer.writeInt(i.getCrc());
				buffer.writeInt(i.getRevision());
			}

			compressed = compress(CompressionType.NONE, Arrays.copyOf(buffer.array(), buffer.readableBytes()));
		}
		else
		{
			Index i = store.findIndex(archiveId);
			assert i != null;

			byte[] indexData = i.toIndexData().writeIndexData();

			compressed = compress(CompressionType.NONE, indexData);
		}

		ArchiveResponsePacket response = new ArchiveResponsePacket();
		response.setIndex(index);
		response.setArchive(archiveId);
		response.setData(compressed);

		ctx.writeAndFlush(response);
	}

	private void handleRequest(ChannelHandlerContext ctx, int index, int archiveId) throws IOException
	{
		logger.info("Client {} requests index {} archive {}", ctx.channel().remoteAddress(), index, archiveId);

		Index i = store.findIndex(index);
		assert i != null;

		Archive archive = i.getArchive(archiveId);
		assert archive != null;

		byte[] packed;
		if (archive.getData() != null)
		{
			packed = archive.getData(); // is compressed, includes length and type

			byte compression = packed[0];
			int compressedSize = Ints.fromBytes(packed[1], packed[2],
				packed[3], packed[4]);

			assert packed.length == 1 // compression
				+ 4 // compressed size
				+ compressedSize
				+ (compression != CompressionType.NONE ? 4 : 0)
				: "maybe revision is at end of data?";
		}
		else
		{
			byte[] data = archive.saveContents();
			packed = compress(archive.getCompression(), data);
		}

		ArchiveResponsePacket response = new ArchiveResponsePacket();
		response.setIndex(index);
		response.setArchive(archiveId);
		response.setData(packed);

		ctx.writeAndFlush(response);
	}

	private byte[] compress(int compression, byte[] data) throws IOException
	{
		return DataFile.compress(data, compression, -1, null);
	}
}
