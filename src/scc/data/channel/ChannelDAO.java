package scc.data.channel;

import java.util.Arrays;

public class ChannelDAO {

	private String _rid;
	private String _ts;
	private String id;
	private String name;
	private String owner;
	private boolean publicChannel;
	private String[] members;

	public ChannelDAO() {
	}

	public ChannelDAO(Channel channel) {
		this(channel.getId(), channel.getName(), channel.getOwner(), channel.getVisibility(), channel.getMembers());
	}

	public ChannelDAO(String id, String name, String owner, boolean publicChannel, String[] members) {
		super();
		this.id = id;
		this.name = name;
		this.members = members;
		this.publicChannel = publicChannel;
		this.owner = owner;
	}

	public String get_rid() {
		return _rid;
	}

	public void set_rid(String _rid) {
		this._rid = _rid;
	}

	public String get_ts() {
		return _ts;
	}

	public void set_ts(String _ts) {
		this._ts = _ts;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getname() {
		return name;
	}

	public void setname(String name) {
		this.name = name;
	}

	public String getOwner() {
		return owner;
	}

	public void setOwner(String owner) {
		this.owner = owner;
	}

	public String[] getMembers() {
		return members;
	}

	public void setMembers(String[] members) {
		this.members = members;
	}

	public void setVisibility(boolean publicChannel) {
		this.publicChannel = publicChannel;
	}

	public boolean getVisibility() {
		return publicChannel;
	}

	public Channel toChannel() {
		return new Channel(id, name, owner, publicChannel, members);
	}

	@Override
	public String toString() {
		return "ChannelDAO [_rid=" + _rid + ", _ts=" + _ts + ", id=" + id + ", name=" + name + ", owner=" + owner
				+ ", publicChannel=" + publicChannel + ", usersIds=" + Arrays.toString(members) + "]";
	}

}
