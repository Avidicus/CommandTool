package com.thelincolnshome.CommandTool.DNS;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.validator.routines.DomainValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xbill.DNS.ARecord;
import org.xbill.DNS.Address;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.NSRecord;
import org.xbill.DNS.Record;
import org.xbill.DNS.TextParseException;
import org.xbill.DNS.Type;

import com.thelincolnshome.CommandTool.Register;

public class Domain
{
	private static Logger					log				= LoggerFactory.getLogger(Domain.class);

	private String							hostname		= null;
	private ArrayList<IPv4>					aRecords		= new ArrayList<IPv4>();
	private ArrayList<String>				nameServers		= new ArrayList<String>();

	private static final ArrayList<IPv4>	STORESONLINE	= new ArrayList<IPv4>();

	@Register
	public static void initialize()
	{
		try
		{
			STORESONLINE.add(new IPv4("208.187.218.0/24"));
			STORESONLINE.add(new IPv4("209.210.220.0/24"));
			STORESONLINE.add(new IPv4("184.178.213.0/24"));
			STORESONLINE.add(new IPv4("70.102.216.0/24"));
			STORESONLINE.add(new IPv4("70.102.218.0/24"));
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}

	public Domain(String inHostname)
	{
		if(DomainValidator.getInstance().isValid(inHostname))
		{
			hostname = inHostname;
		}
		else
		{
			throw new IllegalArgumentException("Domain is not valid");
		}
	}

	public List<String> getNameServers()
	{
		if(nameServers.isEmpty())
		{
			try
			{
				for(Record record : lookupRecords(hostname, Type.NS))
				{
					NSRecord ns = (NSRecord) record;
					nameServers.add(ns.getTarget().toString());
				}
			}
			catch(Exception e)
			{
				log.error("Unable to lookup name", e);
			}
		}

		return nameServers;
	}

	public String getHostname()
	{
		return hostname;
	}

	private List<Record> lookupRecords(String inHostname, int inType) throws TextParseException
	{
		Record[] records = new Lookup(inHostname, inType).run();

		if(records == null)
		{
			return Collections.emptyList();
		}

		return Arrays.asList(records);
	}

	public List<IPv4> getARecords()
	{
		if(aRecords.isEmpty())
		{
			try
			{
				for(Record record : lookupRecords(hostname, Type.A))
				{
					ARecord a = (ARecord) record;

					aRecords.add(new IPv4(a.getAddress().getHostAddress()));
				}
			}
			catch(Exception e)
			{
				log.error("Unable to lookup name", e);
			}
		}

		return aRecords;
	}

	public boolean isStoresOnlineHosted()
	{
		for(IPv4 ipaddress : aRecords)
		{
			if(inNetwork(ipaddress))
			{
				return true;
			}
		}

		return false;
	}

	public boolean isStoresOnlineNameServers()
	{
		for(String ns : getNameServers())
		{
			try
			{
				IPv4 address = new IPv4(Address.getByName(ns).getHostAddress());

				if(inNetwork(address))
				{
					return true;
				}
			}
			catch(Exception e)
			{
				log.error("Unable to lookup name", e);
			}
		}

		return false;
	}

	private boolean inNetwork(IPv4 address)
	{
		for(IPv4 network : STORESONLINE)
		{
			if(network.includes(address))
			{
				return true;
			}
		}

		return false;
	}
}
