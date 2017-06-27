#!/bin/bash

set -ex

unset _JAVA_OPTIONS

readonly manifest="src/main/AndroidManifest.xml"
readonly code=$(Xtract /manifest/@android:versionCode "$manifest")
readonly nextcode=$(("$code" + 1))

readonly version="0.${code}"
readonly nextversion="0.${code}+"

readonly tag="v${version}"

readonly releasedir="../locus-rflkt-addon-release-$tag"

sed -i -e 's/android:versionName="[0-9.+]*"/android:versionName="'"$version"'"/' "$manifest"
git commit -m 'Release '"$version" -- "$manifest"
git tag -s "$tag"

sed -i -e 's/android:versionCode="[0-9]*"/android:versionCode="'"$nextcode"'"/' "$manifest"
sed -i -e 's/android:versionName="[0-9.+]*"/android:versionName="'"$nextversion"'"/' "$manifest"
git commit -m 'Bump version for further development' -- "$manifest"

git worktree add "$releasedir" "$tag"
ln -sr local.properties "$releasedir"
(cd "$releasedir" && sbt android:packageRelease)
cp "$releasedir"/target/android/output/locus-rflkt-addon-release.apk ./releases/locus-rflkt-addon-"$tag".apk
cp "$releasedir"/target/android/intermediates/proguard/mappings.txt ./releases/locus-rflkt-addon-"$tag".mapping.txt
rm -rf "$releasedir"
git worktree prune

git push origin master "$tag"
