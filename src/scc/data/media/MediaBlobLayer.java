package scc.data.media;

import javax.servlet.ServletContext;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.WebApplicationException;

import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobContainerClientBuilder;
import scc.mgt.AzureProperties;

public class MediaBlobLayer {

	private static MediaBlobLayer instance;

	public static MediaBlobLayer getInstance(ServletContext context) {
		if(instance == null) {
			BlobContainerClient containerClient = new BlobContainerClientBuilder()
					.connectionString(AzureProperties.getProperty(context, "BlobStoreConnection"))
					.containerName("images")
					.buildClient();
			instance = new MediaBlobLayer(context, containerClient);
		}

		return instance;
	}

	private final ServletContext context;
	private final BlobContainerClient containerClient;

	private MediaBlobLayer(ServletContext context, BlobContainerClient containerClient) {
		this.context = context;
		this.containerClient = containerClient;
	}

	public void upload(String id, byte[] contents) {
		BlobClient blob = containerClient.getBlobClient(id);
		if(!blob.exists())
			blob.upload(BinaryData.fromBytes(contents));
	}
	
	public byte[] download(String id) {
		BlobClient result = containerClient.getBlobClient(id);
		if(!result.exists())
			throw new WebApplicationException(404);

		return result.downloadContent().toBytes();	
	}

	public void delete(String id) {
		BlobClient result = containerClient.getBlobClient(id);
		result.delete();
	}
}
