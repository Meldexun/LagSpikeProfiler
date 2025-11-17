package meldexun.lagspikeprofiler.command;

import java.util.concurrent.TimeUnit;

import meldexun.lagspikeprofiler.profiler.ProfilingManager;
import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.CommandNotFoundException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;

public class CommandProfile extends CommandBase {

	@Override
	public String getName() {
		return "profile";
	}

	@Override
	public String getUsage(ICommandSender sender) {
		return "/profile time";
	}

	@Override
	public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
		if (args.length == 0) {
			throw new CommandNotFoundException();
		}
		int seconds = parseInt(args[0], 0, 100);
		if (((ProfilingManager) Minecraft.getMinecraft()).tryStart(TimeUnit.SECONDS.toMillis(seconds))) {
			sender.sendMessage(new TextComponentString("Starting profiler for " + seconds + " seconds."));
		} else {
			sender.sendMessage(new TextComponentString("Can't start profiler because profiler already running!"));
		}
	}

}
