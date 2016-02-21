res:
	for i in mdpi.24 hdpi.36 xhdpi.48 xxhdpi.72 xxxhdpi.96; do convert -resize $${i#*.} ic_notification.png ../src/main/res/drawable-$${i%.*}/ic_notification.png; done
	for i in mdpi.48 hdpi.72 xhdpi.96 xxhdpi.144 xxxhdpi.192; do convert -resize $${i#*.} ic_launcher.png ../src/main/res/drawable-$${i%.*}/ic_launcher.png; done
