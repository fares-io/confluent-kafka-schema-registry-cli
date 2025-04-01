package io.fares.bind.maven;

import java.net.URLClassLoader;

public interface ClassLoaderProvider {

  URLClassLoader getClassLoader();

}
