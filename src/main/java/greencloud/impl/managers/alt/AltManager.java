package greencloud.impl.managers.alt;

import greencloud.GreenCloud;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Session;

import java.io.*;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AltManager {
    private final List<Alt> alts = new ArrayList<>();
    private Alt currentAlt;
    private final File altFile;

    public AltManager() {
        this.altFile = new File(GreenCloud.altsDir, "alts.txt");
        loadAlts();
    }

    public void login(Alt alt) {
        for (Alt a : alts) a.setStatus(Alt.Status.Login);

        if (alt.isMicrosoft()) {
            MicrosoftLogin.loginToMinecraft(alt.getData()).thenAccept(session -> {
                if (session == null && alt.getRefreshToken() != null) {
                    MicrosoftLogin.refresh(alt.getRefreshToken()).thenAccept(tokens -> {
                        if (tokens != null) {
                            alt.setData(tokens.get("access_token"));
                            alt.setRefreshToken(tokens.get("refresh_token"));
                            saveAlts();
                            MicrosoftLogin.loginToMinecraft(alt.getData()).thenAccept(s -> finalizeLogin(alt, s));
                        }
                    });
                } else if (session != null) {
                    finalizeLogin(alt, session);
                }
            });
        } else {
            finalizeLogin(alt, new Session(alt.getUsername(), "", alt.getData(), "mojang"));
        }
    }

    private void finalizeLogin(Alt alt, Session session) {
        setSession(session);
        alt.setStatus(Alt.Status.LoggedIn);
        this.currentAlt = alt;
    }

    private void setSession(Session session) {
        try {
            Field field = Minecraft.class.getDeclaredField("session");
            field.setAccessible(true);
            field.set(Minecraft.getMinecraft(), session);
        } catch (Exception e) {
            try {
                Field field = Minecraft.class.getDeclaredField("field_71449_j");
                field.setAccessible(true);
                field.set(Minecraft.getMinecraft(), session);
            } catch (Exception ignored) {}
        }
    }

    public void saveAlts() {
        try (PrintWriter writer = new PrintWriter(new FileWriter(altFile))) {
            for (Alt alt : alts) {
                writer.println(alt.getType().name() + ":" + alt.getData() + ":" + alt.getUsername() + ":" + (alt.getRefreshToken() != null ? alt.getRefreshToken() : "none"));
            }
        } catch (IOException e) {}
    }

    public void loadAlts() {
        if (!altFile.exists()) return;
        alts.clear();
        try (BufferedReader reader = new BufferedReader(new FileReader(altFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] args = line.split(":");
                if (args.length >= 2) {
                    Alt.AccountType type = Alt.AccountType.valueOf(args[0]);
                    Alt alt = new Alt(args[1], args[2], type, Alt.Status.Login);
                    if (args.length > 3 && !args[3].equals("none")) alt.setRefreshToken(args[3]);
                    alts.add(alt);
                }
            }
        } catch (Exception e) {}
    }

    public Alt getCurrentAlt() { return currentAlt; }

    public List<Alt> getAlts() { return alts; }

    public void addAlt(Alt alt) {
        alts.add(alt);
        saveAlts();
    }

    public void removeAlt(Alt alt) {
        alts.remove(alt);
        saveAlts();
    }
}