package scc.data.media;

import java.util.List;
import java.util.stream.Collectors;

import javax.ws.rs.NotFoundException;

import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobContainerClientBuilder;
import com.azure.storage.blob.models.BlobItem;
import scc.mgt.AzureProperties;

public class MediaBlobLayer {

	private static MediaBlobLayer instance;

	public static MediaBlobLayer getInstance() {
		if(instance == null) {
			BlobContainerClient containerClient = new BlobContainerClientBuilder()
					.connectionString(AzureProperties.getProperty("BlobStoreConnection"))
					.containerName("images")
					.buildClient();
			instance = new MediaBlobLayer(containerClient);
		}

		return instance;
	}

	private final BlobContainerClient containerClient;

	private MediaBlobLayer(BlobContainerClient containerClient) {
		this.containerClient = containerClient;
	}
	
	public void upload(String id, byte[] contents) {
		BlobClient blob = containerClient.getBlobClient(id);
		blob.upload(BinaryData.fromBytes(contents));
	}
	
	public byte[] download(String id) {
		BlobClient result = containerClient.getBlobClient(id);
		if(!result.exists())
			throw new NotFoundException();

		return result.downloadContent().toBytes();	
	}

	//TODO delete not needed?
	public List<String> list() {
		return containerClient.listBlobs().stream().map(BlobItem::getName).collect(Collectors.toList());
	}
}
