package scc.data.media;

import javax.ws.rs.WebApplicationException;
import java.io.*;

public class MediaVolumeLayer {

    private final String path = "/mnt/vol";

    public MediaVolumeLayer() {
    }

    public void upload(String id, byte[] contents) {
        try {
            File file = new File(path + id);
            if (!file.createNewFile()) {
                throw new WebApplicationException(409);
            }
            try (FileOutputStream outputStream = new FileOutputStream(file)) {
                outputStream.write(contents);
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new WebApplicationException(500);
        }
    }

    public byte[] download(String id) {
        File file = new File(path + id);
        try (FileInputStream inputStream = new FileInputStream(file)) {
            byte[] bytes = new byte[(int) file.length()];
            inputStream.read(bytes);
            return bytes;
        } catch (FileNotFoundException e) {
            throw new WebApplicationException(404);
        } catch (IOException e) {
            e.printStackTrace();
            throw new WebApplicationException(500);
        }
    }

    public void delete(String id) {
        File file = new File(path + id);
        if(!file.delete()) {
            throw new WebApplicationException(404);
        }
    }
}