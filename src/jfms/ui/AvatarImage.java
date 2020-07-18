package jfms.ui;

import java.io.ByteArrayInputStream;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import jfms.config.Config;
import jfms.fms.FmsManager;
import jfms.fms.IdentityManager;

public class AvatarImage {
	final ImageView imageView = new ImageView();
	final Image fallbackImage;
	final IdentityManager identityManager;
	final int avatarSize;
	int identityId = -1;

	public AvatarImage(boolean useFallback) {
		identityManager = FmsManager.getInstance().getIdentityManager();
		Image genericAvatar = Icons.getInstance().getGenericAvatar();
		if (genericAvatar != null) {
			avatarSize = (int)genericAvatar.getWidth();
		} else {
			avatarSize = 75;
		}

		if (useFallback) {
			fallbackImage = genericAvatar;
		} else {
			fallbackImage = null;
		}
	}

	public ImageView getImageView() {
		return imageView;
	}

	public double getSize() {
		return avatarSize;
	}

	public void setIdentityId(int identityId) {
		if (!Config.getInstance().getShowAvatars()) {
			return;
		}

		if (identityId == this.identityId) {
			return;
		}

		this.identityId = identityId;


		byte[] data = null;
		if (identityId != -1) {
			data = identityManager.getAvatar(identityId);
		}

		Image img;
		if (data != null) {
			img = new Image(new ByteArrayInputStream(data),
					avatarSize, avatarSize, true, true);
			if (img.isError()) {
				img = fallbackImage;
			}
		} else {
			img = fallbackImage;
		}

		if (img != imageView.getImage()) {
			imageView.setImage(img);
		}
	}
}
