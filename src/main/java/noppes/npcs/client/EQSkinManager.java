package noppes.npcs.client;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.ResourceLocation;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

@SideOnly(Side.CLIENT)
public class EQSkinManager {

    private static final Map<String, ResourceLocation> CACHE = new HashMap<>();

    private static final Map<String, Long> FAILED = new HashMap<>();

    private static final long RETRY_DELAY = 60_000; // 1 minute

    private static final ResourceLocation STEVE_SKIN = new ResourceLocation("textures/entity/steve.png");

    private EQSkinManager() {
    }

    public static ResourceLocation get(String username) {
        if (username == null || username.isEmpty())
            return STEVE_SKIN;

        username = username.toLowerCase();

        if (CACHE.containsKey(username))
            return CACHE.get(username);

        if (FAILED.containsKey(username)) {
            long lastFail = FAILED.get(username);
            if (System.currentTimeMillis() - lastFail < RETRY_DELAY) {
                return STEVE_SKIN;
            }
            FAILED.remove(username);
        }

        ResourceLocation loc = download(username);
        if (loc != null) {
            CACHE.put(username, loc);
            FAILED.remove(username);
            return loc;
        }

        return STEVE_SKIN;
    }

    public static void clear(String username) {
        if (username == null)
            return;
        CACHE.remove(username.toLowerCase());
        FAILED.remove(username.toLowerCase());
    }

    private static ResourceLocation download(String username) {
        try {
            URL url = new URL("https://api.earthquest.fr/api/v1/public/skins/" + username + "/skin.png");
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            con.setConnectTimeout(5000);
            con.setReadTimeout(5000);
            con.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
            con.setRequestProperty("Accept", "image/*,*/*;q=0.8");

            if (con.getResponseCode() != 200)
                return null;

            InputStream in = con.getInputStream();
            BufferedImage image = ImageIO.read(in);
            in.close();

            if (image == null)
                return null;

            if (image.getHeight() == 64) {
                BufferedImage fixed = new BufferedImage(64, 32, BufferedImage.TYPE_INT_ARGB);
                Graphics graphics = fixed.getGraphics();

                graphics.drawImage(image, 0, 0, 64, 32, 0, 0, 64, 32, null);
                graphics.drawImage(image, 32, 0, 64, 16, 32, 0, 64, 16, null);
                graphics.drawImage(image, 16, 16, 40, 32, 16, 32, 40, 48, null);
                graphics.drawImage(image, 40, 16, 56, 32, 40, 32, 56, 48, null);
                graphics.drawImage(image, 0, 16, 16, 32, 0, 32, 16, 48, null);

                graphics.dispose();
                image = fixed;
            }


            DynamicTexture dyn = new DynamicTexture(image);
            TextureManager tm = Minecraft.getMinecraft().getTextureManager();

            return tm.getDynamicTextureLocation(
                "earthquest/" + username,
                dyn
            );

        } catch (Exception e) {
            // volontairement silencieux pour ne pas spammer la console
            return null;
        }
    }
}
