package scc.srv;

import scc.resources.AuthenticationResource;
import scc.resources.*;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Application;

public class MainApplication extends Application
{
	private Set<Object> singletons = new HashSet<Object>();
	private Set<Class<?>> resources = new HashSet<Class<?>>();

	public MainApplication() {
		resources.add(ControlResource.class);

		resources.add(MediaResource.class);
		singletons.add(new MediaResource());

		resources.add(MessageResource.class);
		singletons.add(new MessageResource());

		resources.add(ChannelResource.class);
		singletons.add(new ChannelResource());

		resources.add(UsersResource.class);
		singletons.add(new UsersResource());

		resources.add(AuthenticationResource.class);
		singletons.add(new AuthenticationResource());

		resources.add(TrendResource.class);
		singletons.add(new TrendResource());
	}

	@Override
	public Set<Class<?>> getClasses() {
		return resources;
	}

	@Override
	public Set<Object> getSingletons() {
		return singletons;
	}
}
