package com.thelincolnshome.CommandTool;

import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main
{
	protected static final Logger			log			= LoggerFactory.getLogger(Main.class);
	protected static HashMap<String, File>	jars		= new HashMap<String, File>();

	public static final HashSet<Method>		registers	= new HashSet<Method>();
	protected static MainClassLoader		classLoader;
	protected static File					root;

	public static void main(String[] args)
	{
		try
		{
			register();

			if(args.length == 1 && StringUtils.isNotEmpty(args[0]))
			{
				LineParser interpreter = LineParsers.getParser(args[0]);

				if(interpreter != null)
				{
					Console console = new Console(interpreter);
					console.run();

					return;
				}
			}

			Console.display("Available Parsers: ");
			Console.display(LineParsers.getParsers());
			Console.display("\n");
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}

	static void register() throws Exception
	{
		File libs = new File(root, ".");

		// ----------------

		findJars(libs);

		// ----------------

		classLoader = new MainClassLoader();

		Thread.currentThread().setContextClassLoader(classLoader);

		classLoader.add(libs.toURI().toURL()); // Required to load other resources in the libs dir (TSAPI.PRO)

		// ----------------

		for(File file : getJarFiles())
		{
			JarFile jarFile = null;

			try
			{
				jarFile = new JarFile(file);

				for(Enumeration<JarEntry> enumeration = jarFile.entries(); enumeration.hasMoreElements();)
				{
					JarEntry entry = enumeration.nextElement();

					String name = entry.getName();

					if(name.endsWith(".class"))
					{
						name = name.substring(0, name.length() - 6).replaceAll("\\/", ".");

						try
						{
							Class<?> registerClass = classLoader.loadClass(name);

							if(registerClass != null)
							{
								for(Method method : registerClass.getMethods())
								{
									if(method.isAnnotationPresent(Register.class))
									{
										registers.add(method);
									}
								}
							}
						}
						catch(NoClassDefFoundError e)
						{
						}
						catch(Throwable e)
						{
							throw new Exception(e);
						}
					}
				}
			}
			catch(Exception e)
			{
				throw e;
			}
			finally
			{
				if(jarFile != null)
				{
					try
					{
						jarFile.close();
					}
					catch(Exception e)
					{
					}
				}
			}
		}

		for(Method method : registers)
		{
			try
			{
				if((method.getModifiers() & Modifier.STATIC) == 0)
				{
					throw new Exception("Register methods must be static");
				}

				if(method.getParameterTypes().length > 0)
				{
					throw new Exception("Register methods can not have parameters");
				}

				method.invoke(null);
			}
			catch(Throwable e)
			{
				throw new Exception(e);
			}
		}

		registers.clear();
	}

	static void findJars(File inDirectory) throws Exception
	{
		if(inDirectory.exists())
		{
			File[] files = inDirectory.listFiles();

			for(File file : files)
			{
				if(file.isDirectory())
				{
					findJars(file);
				}
				else if(file.getName().endsWith(".jar"))
				{
					jars.put(file.getName(), file);
				}
			}
		}
	}

	public static File[] getJarFiles()
	{
		return jars.values().toArray(new File[jars.values().size()]);
	}

	protected static class MainClassLoader extends URLClassLoader
	{
		protected MainClassLoader()
		{
			super(new URL[0], Thread.currentThread().getContextClassLoader());
		}

		protected void add(URL inURL)
		{
			super.addURL(inURL);
		}
	}
}
