package com.thelincolnshome.CommandTool.DNS;

import java.util.HashMap;

import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IPv4 extends Object
{
	protected static final Logger	log				= LoggerFactory.getLogger(IPv4.class);

	public final static int			FULL_CIDR		= 32;
	public final static long		DEFAULT_MASK	= 0x00000000FFFFFFFF;

	public final static String		NETWORK			= "NETWORK";
	public final static String		BROADCAST		= "BROADCAST";

	protected long					addr_network	= 0;
	protected long					addr_broadcast	= 0;

	protected int					cidr			= 0;
	protected long					mask			= DEFAULT_MASK;


	public IPv4(long addr_network) throws Exception
	{
		parseAddressBlock(addr_network, addr_network);
	}

	public IPv4(long addr_network, long addr_broadcast) throws Exception
	{
		parseAddressBlock(addr_network, addr_broadcast);
	}

	public IPv4(Integer addr_network) throws Exception
	{
		parseAddressBlock(addr_network.longValue(), addr_network.longValue());
	}

	public IPv4(Integer addr_network, Integer addr_broadcast) throws Exception
	{
		parseAddressBlock(addr_network.longValue(), addr_broadcast.longValue());
	}

	public IPv4(String addr_str) throws Exception
	{
		String cidr_str = "0";
		String dotted_quad = addr_str.trim();

		cidr = FULL_CIDR;

		// attempt to parse a network mask value

		if(addr_str.indexOf("/") >= 0)
		{
			java.util.StringTokenizer st = new java.util.StringTokenizer(addr_str, "/", false);
			String token = st.nextToken().trim();

			if(token.length() > 0)
			{
				dotted_quad = token;
			}

			if(st.hasMoreTokens())
			{
				token = st.nextToken().trim();

				if(token.length() > 0)
				{
					cidr_str = token;
				}
			}

			cidr = Integer.parseInt(cidr_str);

			calculateMask();
		}

		long delta = (1 << (FULL_CIDR - cidr)) - 1;

		addr_network = quadToLong(dotted_quad) & mask;
		addr_broadcast = addr_network + delta;

		if(log.isTraceEnabled())
		{
			log.trace("addr=" + Long.toHexString(addr_network) + " - " + Long.toHexString(addr_broadcast) + ", mask=" + Long.toHexString(mask) + ", cidr=" + cidr + ", delta=" + delta);
		}
	}

	public IPv4(String addr_network, String addr_broadcast) throws Exception
	{
		parseAddressBlock(quadToLong(addr_network), quadToLong(addr_broadcast));
	}

	public static IPv4 mask2cidr(String ip_addr_str, String net_mask_str) throws Exception
	{
		IPv4 ip_addr = new IPv4(ip_addr_str);
		IPv4 net_mask = new IPv4(net_mask_str);

		long nm = ip_addr.getNetworkAddr() & net_mask.getNetworkAddr();
		long bc = nm + (0xFFFFFFFF ^ net_mask.getNetworkAddr());

		IPv4 cidr = new IPv4(longToQuad(nm), longToQuad(bc));

		return cidr;
	}

	protected void parseAddressBlock(long addr_network, long addr_broadcast) throws Exception
	{
		this.addr_network = addr_network;
		this.addr_broadcast = addr_broadcast;

		if(addr_network < 0)
		{
			throw new Exception("invalid network address value: " + addr_network);
		}
		else if(addr_broadcast < 0)
		{
			throw new Exception("invalid broadcast address value: " + addr_broadcast);
		}
		else if(addr_network > addr_broadcast)
		{
			throw new Exception("network address cannot be higher than broadcast address");
		}
		else if(addr_network == addr_broadcast)
		{
			cidr = FULL_CIDR;
			//mask = DEFAULT_MASK
		}
		else
		{
			cidr = FULL_CIDR - (int) (Math.log((addr_broadcast - addr_network + 1)) / Math.log(2.0));
			calculateMask();
		}

		if(log.isTraceEnabled())
		{
			log.trace("addr=" + Long.toHexString(addr_network) + " - " + Long.toHexString(addr_broadcast) + ", mask=" + Long.toHexString(mask) + ", cidr=" + cidr);
		}
	}

	public void calculateMask()
	{
		if((cidr != 0xFFFFFFFF) && (cidr > 0))
		{
			mask = 0xFFFFFFFF << (FULL_CIDR - cidr);
		}
	}

	public boolean includes(IPv4 test_addr)
	{
		return includes(test_addr.getNetworkAddr());
	}

	public boolean includes(long test_addr)
	{
		if(log.isTraceEnabled())
		{
			log.trace("addr=" + Long.toHexString(addr_network) + ", mask=" + Long.toHexString(mask) + ", test=" + Long.toHexString(test_addr));
		}

		return ((mask & addr_network) == 0) || ((test_addr & mask) == addr_network);
	}

	public long getNetworkAddr()
	{
		return addr_network;
	}

	public long getBroadcastAddr()
	{
		return addr_broadcast;
	}

	public int getCIDR()
	{
		return cidr;
	}

	@Override
	public String toString()
	{
		return getString();
	}

	public String getString()
	{
		String s = longToQuad(addr_network);

		if((cidr > 0) && (cidr < FULL_CIDR))
		{
			s += "/" + cidr;
		}

		return s;
	}

	@Override
	public int hashCode()
	{
		return getString().hashCode();
	}

	@Override
	public boolean equals(Object obj)
	{
		return (obj instanceof IPv4) && getString().equals(((IPv4) obj).getString());
	}

	public static long quadToLong(String dotted_quad)
	{
		long addr = 0;

		String[] parts = dotted_quad.split("\\.");

		if(parts.length != 4)
		{
			log.error("Invalid IP Address (parts != 4): " + dotted_quad);

			return addr;
		}

		for(String part : parts)
		{
			long quad = NumberUtils.createInteger(part);

			if(quad > 255 | quad < 0)
			{
				quad = 0;
			}

			addr = (addr << 8) | quad;
		}

		return addr;
	}

	public static String longToQuad(long addr)
	{
		StringBuilder s = new StringBuilder();
		s.append(Long.toString((addr >> 24) & 0xFF));
		s.append(".").append(Long.toString((addr >> 16) & 0xFF));
		s.append(".").append(Long.toString((addr >> 8) & 0xFF));
		s.append(".").append(Long.toString(addr & 0xFF));

		return s.toString();
	}

	public static long hexToLong(String addr)
	{
		return Long.valueOf(addr, 16).longValue();
	}

	public static String longToHex(long addr)
	{
		return Long.toHexString(addr);
	}

	public static IPv4 parseHex(String addr_network) throws Exception
	{
		return new IPv4(hexToLong(addr_network));
	}

	public static IPv4 parseHex(String addr_network, String addr_broadcast) throws Exception
	{
		return new IPv4(hexToLong(addr_network), hexToLong(addr_broadcast));
	}

	public static boolean cidrToLong(String cidr_addr, long[] addr_network, long[] addr_broadcast)
	{
		boolean result = true;

		try
		{
			IPv4 ip = new IPv4(cidr_addr);
			addr_network[0] = ip.getNetworkAddr();
			addr_broadcast[0] = ip.getBroadcastAddr();
		}
		catch(Exception e)
		{
			if(log.isDebugEnabled())
			{
				log.debug(e.getMessage(), e);
			}

			result = false;
		}

		return result;
	}

	public static boolean cidrToHex(String cidr_addr, String[] addr_network, String[] addr_broadcast)
	{
		boolean result = true;

		try
		{
			IPv4 ip = new IPv4(cidr_addr);
			addr_network[0] = longToHex(ip.getNetworkAddr());
			addr_broadcast[0] = longToHex(ip.getBroadcastAddr());
		}
		catch(Exception e)
		{
			if(log.isDebugEnabled())
			{
				log.debug(e.getMessage(), e);
			}

			result = false;
		}

		return result;
	}

	public static boolean longToCidr(long addr_network, long addr_broadcast, String[] cidr_addr)
	{
		boolean result = true;

		try
		{
			IPv4 ip = new IPv4(addr_network, addr_broadcast);
			cidr_addr[0] = ip.toString();
		}
		catch(Exception e)
		{
			if(log.isDebugEnabled())
			{
				log.debug(e.getMessage(), e);
			}

			result = false;
		}

		return result;
	}

	public static boolean ipToHex(String ip_addr, String[] hex_addr)
	{
		boolean result = true;

		try
		{
			IPv4 ip = new IPv4(ip_addr);
			hex_addr[0] = longToHex(ip.getNetworkAddr());
		}
		catch(Exception e)
		{
			if(log.isDebugEnabled())
			{
				log.debug(e.getMessage(), e);
			}

			result = false;
		}

		return result;
	}

	public static boolean hexToIp(String hex_addr, String[] ip_addr)
	{
		boolean result = true;

		try
		{
			IPv4 ip = new IPv4(hexToLong(hex_addr));
			ip_addr[0] = ip.toString();
		}
		catch(Exception e)
		{
			if(log.isDebugEnabled())
			{
				log.debug(e.getMessage(), e);
			}

			result = false;
		}

		return result;
	}

	public static String ips2cidr(long ip_begin_long, long ip_broadcast_long) throws Exception
	{
		//String cidr_string = "";
		IPv4 ip = new IPv4(ip_begin_long, ip_broadcast_long);
		return ip.toString();
	}

	public static String ips2cidr(String ip_begin_string, String ip_broadcast_string) throws Exception
	{
		return ips2cidr(IPv4.quadToLong(ip_begin_string), IPv4.quadToLong(ip_broadcast_string));
	}

	public static HashMap<String, String> cidr2ips(String cidr_string)
	{
		HashMap<String, String> result = new HashMap<String, String>();

		String ip_network = null;
		String ip_broadcast = null;
		try
		{
			IPv4 ip_source = new IPv4(cidr_string);
			ip_broadcast = new IPv4(ip_source.getBroadcastAddr()).toString();
			ip_network = new IPv4(ip_source.getNetworkAddr()).toString();

		}
		catch(Exception e)
		{
			throw new IllegalArgumentException(cidr_string + " is not a valid network.");
		}

		result.put(IPv4.NETWORK, ip_network);
		result.put(IPv4.BROADCAST, ip_broadcast);

		return result;
	}

	public static void main(String args[]) throws Exception
	{
		String ip_addr_str = "10.1.2.8";
		String net_mask_str = "255.255.255.252";

		IPv4 ip_v4_addr = new IPv4(ip_addr_str);
		IPv4 net_mask = new IPv4(net_mask_str);

		long nm = ip_v4_addr.getNetworkAddr() & net_mask.getNetworkAddr();
		long bc = nm + (0xFFFFFFFF ^ net_mask.getNetworkAddr());

		IPv4 cidr = new IPv4(longToQuad(nm), longToQuad(bc));

		System.err.println("ip_addr: " + ip_v4_addr.toString());
		System.err.println("net_mask: " + net_mask.toString());
		System.err.println("net_addr: " + longToQuad(nm));
		System.err.println("broadcast: " + longToQuad(bc));

		System.err.println("cidr: " + cidr.toString());
		System.err.println("cidr: " + mask2cidr(ip_addr_str, net_mask_str));

		/**
		 * @TODO previous unit tests
		 *
		 */

		IPv4 z = new IPv4("10.1.2.8");
		long test_addr = (new IPv4("10.1.2.0")).getNetworkAddr();

		System.err.println("str  = " + z.getString());
		System.err.println("long = " + z.getNetworkAddr());
		System.err.println("quad = " + longToQuad(z.getNetworkAddr()));
		System.err.println("hex  = " + longToHex(z.getNetworkAddr()));
		System.err.println("stop = " + longToHex(z.getBroadcastAddr()));
		System.err.println("cidr = " + z.getCIDR());
		System.err.println("includes: " + z.includes(test_addr));

		boolean result;

		String hex_addr[] = new String[1];
		result = ipToHex("10.1.2.8", hex_addr);
		System.err.println("ipToHex: " + result + " " + hex_addr[0]);

		String ip_addr[] = new String[1];
		result = hexToIp("a010208", ip_addr);
		System.err.println("hexToIp: " + result + " " + ip_addr[0]);

		String cidr_addr[] = new String[1];
		//result = longToCidr(args[2], args[3], cidr_addr);
		System.err.println("longToCidr: " + result + " " + cidr_addr[0]);

		String addr_network[] = new String[1];
		String addr_broadcast[] = new String[1];
		//result = cidrToLong(args[0], addr_network, addr_broadcast);
		System.err.println("cidrToLong: " + result + " " + addr_network[0] + " - " + addr_broadcast[0]);

	}
}
