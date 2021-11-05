package scc.utils;

import java.util.Locale;

import com.azure.cosmos.util.CosmosPagedIterable;
import com.fasterxml.jackson.databind.ObjectMapper;

import redis.clients.jedis.Jedis;
import scc.cache.Cache;
import scc.data.channel.ChannelDAO;
import scc.data.channel.ChannelsDBLayer;
import scc.data.media.MediaBlobLayer;
import scc.data.message.MessageDAO;
import scc.data.message.MessagesDBLayer;
import scc.data.user.UserDAO;
import scc.data.user.UsersDBLayer;

public class Manager {

	

	private static ChannelsDBLayer channels;
	private static UsersDBLayer users;
	private static MessagesDBLayer messages;
	private static MediaBlobLayer media;
	private static Cache cache;

	public void createUser(String name, String pwd, String pathToPhoto) {
		try {
			ObjectMapper mapper = new ObjectMapper();

			Locale.setDefault(Locale.US);
			String id = "0:" + System.currentTimeMillis();
			String photoId = "1:" + System.currentTimeMillis();
			UserDAO user = new UserDAO(id, name, pwd, photoId, new String[0]);

			try (Jedis jedis = Cache.getCachePool().getResource()) {
				jedis.set("user:" + id, new ObjectMapper().writeValueAsString(user));
			}
			users.putUser(user);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void deleteUser(String name) {
		try {
			Locale.setDefault(Locale.US);

			boolean found = false;
			int cnt = 0;
			int limit = 5;
			// Find ID of the given user
			String userId = "";
			while (!found) {
				CosmosPagedIterable<UserDAO> res = users.getUsers(cnt, limit);
				for (UserDAO user : res) {
					if (user.getName().equals(name)) {
						userId = user.getId();
						found = true;
					}
				}
				cnt += limit;
			}

			try (Jedis jedis = Cache.getCachePool().getResource()) {
				jedis.del(userId);
			}
			changeMessagesFrom(userId);
			users.delUserById(userId);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public void updateUser(String id, String name, String pwd, String pathToPhoto, String[] channels) {
		UserDAO u = null;
		ObjectMapper mapper = new ObjectMapper();
		try (Jedis jedis = Cache.getCachePool().getResource()) {
			String res = jedis.get(id);
			if (res != null) {
				u = mapper.readValue(res, UserDAO.class);
			} else {
				CosmosPagedIterable<UserDAO> user = users.getUserById(id);
				for (UserDAO u2 : user) {
					u2.setName(name);
					u2.setPhotoId(pathToPhoto);
					u2.setPwd(pwd);
					u2.setChannelIds(channels);
					u = u2;
				}
			}
			jedis.set("user:" + id, mapper.writeValueAsString(u));
			users.updateUser(u);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void addMessage(String senderId, String dest, String text, String pathToFile, String channelName,
			String replied) {
		try {
			ObjectMapper mapper = new ObjectMapper();
			Locale.setDefault(Locale.US);
			String photoId = "1:" + System.currentTimeMillis();
			String msgId = "2:" + System.currentTimeMillis();
			String channelId = "";
			boolean found = false;
			int cnt = 0;
			int limit = 5;
			// Find ID of the given channel
			while (!found) {
				CosmosPagedIterable<ChannelDAO> res = channels.getChannels(cnt, limit);
				for (ChannelDAO chn : res) {
					if (chn.getname().equals(channelName)) {
						channelId = chn.getId();
						found = true;
					}
				}
			}

			MessageDAO msg = new MessageDAO(msgId, senderId, dest, text, photoId, channelId, replied);

			try (Jedis jedis = Cache.getCachePool().getResource()) {
				jedis.set("msg:" + msgId, mapper.writeValueAsString(msg));
			}
			messages.putMsg(msg);
			//media.uploadFile(pathToFile);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void deleteMessage(String id) {
		try (Jedis jedis = Cache.getCachePool().getResource()) {
			jedis.del(id);
		} catch (Exception e) {
			e.printStackTrace();
		}
		messages.delMsgById(id);

	}

	public void createChannel(String name, String ownerId, boolean publicChannel) {
		ObjectMapper mapper = new ObjectMapper();
		try {
			Locale.setDefault(Locale.US);
			String channelId = "3:" + System.currentTimeMillis();
			ChannelDAO channel = new ChannelDAO(channelId, name, ownerId, publicChannel, new String[0]);
			
			channels.putChannel(channel);
			CosmosPagedIterable<UserDAO> res = users.getUserById(ownerId);
			for (UserDAO u : res) {
				String[] a = u.getChannelIds();
				a[a.length-1] = name;
				u.setChannelIds(a);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void deleteChannel(String id) {
		ObjectMapper mapper = new ObjectMapper();
		ChannelDAO ch = null;
		try (Jedis jedis = Cache.getCachePool().getResource()) {
			ch = mapper.readValue(jedis.get(id), ChannelDAO.class);
			if (ch != null)
				jedis.del(id);
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (ch == null) {
			for (ChannelDAO c : channels.getChannelById(id)) {
				ch = c;
			}
		}
		//retira o channel eliminado da lista de channels de cada user
		for (String mb : ch.getMembers()) {
			for (UserDAO u : users.getUserById(mb)) {
				u.setChannelIds(removeChannelId(u.getChannelIds(), id));
			}
		}
		channels.delChannelById(id);
	}
	
	public void updateChannel(String id, String name, String ownerId, boolean publicChannel, String[] members) {
		ObjectMapper mapper = new ObjectMapper();
		ChannelDAO ch;
		try (Jedis jedis = Cache.getCachePool().getResource()) {
			ch = mapper.readValue(jedis.get(id), ChannelDAO.class);
			if (ch != null) {
				ch.setname(name);
				ch.setMembers(members);
				ch.setOwner(ownerId);
				ch.setVisibility(publicChannel);
			} else {
				CosmosPagedIterable<ChannelDAO> chs = channels.getChannelById(id);
				for(ChannelDAO c : chs)
					ch = c;
			}
			jedis.set("channel:" + id, mapper.writeValueAsString(ch));
			channels.updateChannel(ch);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void uploadMedia(String pathToFile) {
		//media.uploadFile(pathToFile);
	}
	
	//public byte[] downloadMedia(String id) {
		//return media.downloadFile(id);
	//}

	private String[] removeChannelId(String[] channels, String id) {
		String[] ret = new String[channels.length-1];
		int c=0;
		for(String a : channels) {
			if(!a.equals(id)) {
				ret[c]=channels[c];
				c++;
			}
		}
		return ret;
	}

	private void changeMessagesFrom(String userId) {
		ObjectMapper mapper = new ObjectMapper();
		CosmosPagedIterable<MessageDAO> res = messages.getAllMessages();
		String id;
		for (MessageDAO msg : res) {
			if (msg.getsend().equals(userId)) {
				msg.setsend("Deleted User");
				id = msg.getId();
				try (Jedis jedis = Cache.getCachePool().getResource()) {
					jedis.set("msg:" + id, mapper.writeValueAsString(msg));
				} catch (Exception e) {
					e.printStackTrace();
				}
				messages.updatemsg(msg);
			}
		}
	}
	
}
