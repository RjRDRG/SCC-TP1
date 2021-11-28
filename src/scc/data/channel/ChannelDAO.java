package scc.data.channel;

import java.util.Arrays;

public class ChannelDAO {

	private String _rid;
	private String _ts;
	private String idChannel;
	private String name;
	private String owner;
	private boolean publicChannel;
	private String[] members;
	private boolean garbage;

	public ChannelDAO() {
	}

	public ChannelDAO(Channel channel) {
		this(channel.getIdChannel(), channel.getName(), channel.getOwner(), channel.isPublicChannel(), channel.getMembers());
	}

	public ChannelDAO(String idChannel, String name, String owner, boolean publicChannel, String[] members) {
		super();
		this.idChannel = idChannel;
		this.name = name;
		this.members = members;
		this.publicChannel = publicChannel;
		this.owner = owner;
		this.garbage = false;
	}

	public Channel toChannel() {
		return new Channel(idChannel, name, owner, publicChannel, members);
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

	public String getIdChannel() {
		return idChannel;
	}

	public void setIdChannel(String idChannel) {
		this.idChannel = idChannel;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getOwner() {
		return owner;
	}

	public void setOwner(String owner) {
		this.owner = owner;
	}

	public boolean isPublicChannel() {
		return publicChannel;
	}

	public void setPublicChannel(boolean publicChannel) {
		this.publicChannel = publicChannel;
	}

	public String[] getMembers() {
		return members;
	}

	public void setMembers(String[] members) {
		this.members = members;
	}

	public boolean isGarbage() {
		return garbage;
	}

	public void setGarbage(boolean garbage) {
		this.garbage = garbage;
	}

	@Override
	public String toString() {
		return "ChannelDAO{" +
				"_rid='" + _rid + '\'' +
				", _ts='" + _ts + '\'' +
				", idChannel='" + idChannel + '\'' +
				", name='" + name + '\'' +
				", owner='" + owner + '\'' +
				", publicChannel=" + publicChannel +
				", members=" + Arrays.toString(members) +
				", garbage=" + garbage +
				'}';
	}
}
