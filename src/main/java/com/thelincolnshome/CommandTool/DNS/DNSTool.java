package com.thelincolnshome.CommandTool.DNS;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.UnknownHostException;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.StrMatcher;
import org.apache.commons.lang3.text.StrTokenizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xbill.DNS.ExtendedResolver;
import org.xbill.DNS.Lookup;

import com.thelincolnshome.CommandTool.LineParser;
import com.thelincolnshome.CommandTool.LineParsers;
import com.thelincolnshome.CommandTool.Register;

public final class DNSTool implements LineParser
{
	private static final String	EXIT			= "exit";

	private static final String	RUN_FILE		= "run";

	private static final String	HELP			= "h";

	private static final String	TEST_HOSTNAME	= "test";

	private static Logger		log				= LoggerFactory.getLogger(DNSTool.class);

	private static final String	NAMESERVER		= "nameserver";
	private static final String	DNS_FILE		= "dnsfile";
	private static final String	NAME			= "DNS";

	Options						options			= new Options();

	private String				dnsfile;
	private static String		nameserver		= "8.8.8.8";

	public static String getNameserver()
	{
		return nameserver;
	}

	public static void setNameserver(String inNameserver)
	{
		nameserver = inNameserver;
	}

	@Register
	public static final void registration()
	{
		LineParsers.register(new DNSTool());

		try
		{
			Lookup.setDefaultResolver(new ExtendedResolver(new String[] { "8.8.8.8" }));
		}
		catch(UnknownHostException e)
		{
			e.printStackTrace();
		}
	}

	@SuppressWarnings("static-access")
	public DNSTool()
	{
		Option help = OptionBuilder.withLongOpt("help").withDescription("print Usage").create(HELP);
		Option test = OptionBuilder.hasArg().withDescription("hostname to test").create(TEST_HOSTNAME);
		Option nameserver = OptionBuilder.hasArg().withDescription("nameserver to use").create(NAMESERVER);
		Option dnsFile = OptionBuilder.withArgName("DNS File").hasArg().withDescription("use given file for dns entries").create(DNS_FILE);
		Option exit = new Option(EXIT, EXIT);
		Option run = new Option(RUN_FILE, "run DNS File");

		options.addOption(help);
		options.addOption(dnsFile);
		options.addOption(nameserver);
		options.addOption(test);
		options.addOption(run);
		options.addOption(exit);
	}

	public String getName()
	{
		return NAME;
	}

	public String getUsage()
	{
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp(NAME, options);

		return null;
	}

	public String getHelloPrompt()
	{
		return "";
	}

	public boolean parseInput(String inLine, List<Object> theResult) throws ParseException
	{
		if(theResult == null)
		{
			throw new IllegalArgumentException("Result param cannot be null.");
		}

		if(!theResult.isEmpty())
		{
			throw new IllegalArgumentException("Result param must be empty.");
		}

		if(inLine == null)
		{
			throw new IllegalArgumentException("Line must not be null.");
		}

		StrTokenizer tokenizer = new StrTokenizer(inLine, StrMatcher.spaceMatcher(), StrMatcher.singleQuoteMatcher());
		CommandLineParser parser = new GnuParser();
		CommandLine line = parser.parse(options, tokenizer.getTokenArray());

		if(line.hasOption("h") || line.hasOption(HELP))
		{
			getUsage();
		}

		if(EXIT.equalsIgnoreCase(inLine) || line.hasOption(EXIT))
		{
			return true;
		}

		if(line.hasOption(DNS_FILE))
		{
			dnsfile = line.getOptionValue(DNS_FILE);

			File file = new File(dnsfile);

			if(!file.canRead())
			{
				dnsfile = null;
				theResult.add("Unable to read file!");
			}
		}

		if(line.hasOption(RUN_FILE))
		{
			if(StringUtils.isEmpty(dnsfile))
			{
				theResult.add("No DNS File specified");
			}
			else
			{
				run();
			}
		}

		if(line.hasOption(NAMESERVER))
		{
			try
			{
				ExtendedResolver resolver = new ExtendedResolver(StringUtils.split(line.getOptionValue(NAMESERVER)));

				Lookup.setDefaultResolver(resolver);
			}
			catch(UnknownHostException e)
			{
				theResult.add(e.getMessage());
			}

			nameserver = line.getOptionValue(NAMESERVER);
		}

		if(line.hasOption(TEST_HOSTNAME))
		{
			try
			{
				Domain domain = new Domain(line.getOptionValue(TEST_HOSTNAME));

				theResult.add(domain.getHostname());

				for(String nameServer : domain.getNameServers())
				{
					theResult.add(new StringBuilder("NS\t").append(nameServer));
				}

				for(IPv4 ipaddress : domain.getARecords())
				{
					theResult.add(new StringBuilder("A\t").append(ipaddress.toString()));
				}

				theResult.add(new StringBuilder("Is StoresOnline: ").append(domain.isStoresOnlineHosted()));
				theResult.add(new StringBuilder("Is StoresOnline NS: ").append(domain.isStoresOnlineNameServers()));
			}
			catch(Exception e)
			{
				theResult.add(e.getMessage());
			}
		}

		return false;
	}

	private void run()
	{
		FileWriter out = null;
		PrintWriter print = null;

		try
		{
			out = new FileWriter(new File(dnsfile + ".out"));
			print = new PrintWriter(out);

			print.println(compose("DOMAIN", "A", "HOSTED", "DNS"));

			for(Object line : FileUtils.readLines(new File(dnsfile)))
			{
				if(StringUtils.isEmpty(line.toString()))
				{
					continue;
				}

				System.out.println(line.toString());

				try
				{
					Domain domain = new Domain(line.toString());

					print.println(compose(domain.getHostname(), domain.getARecords().get(0).toString(), Boolean.toString(domain.isStoresOnlineHosted()), Boolean.toString(domain.isStoresOnlineNameServers())));
				}
				catch(Exception e)
				{
					log.error("Problem with " + line.toString(), e);
				}
			}
		}
		catch(IOException e)
		{
			log.error("File exception", e);
		}
		finally
		{
			IOUtils.closeQuietly(print);
			IOUtils.closeQuietly(out);
		}
	}

	private String compose(String ... inString)
	{
		return StringUtils.join(inString, "\t");
	}
}
