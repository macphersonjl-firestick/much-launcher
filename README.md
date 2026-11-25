# MUCH Launcher for Fire TV

A custom 90s home screen for a Fire OS Fire TV Stick. GitHub builds the app for you, and you install it on the stick with Downloader.

## Part 1: Build the app on GitHub (on a computer, about 10 minutes)

1. Go to **github.com** and sign in, or create a free account.
2. Click **+** (top right) → **New repository**.
   - Name: `much-launcher`
   - Set it to **Public**. Downloader can only fetch from a public page.
   - Leave everything else unticked and click **Create repository**.
3. On the new, empty repository page, click the **uploading an existing file** link.
4. Unzip `much-launcher.zip` on your computer and open the `much-launcher` folder.
   Select **everything inside it** and drag it all onto the GitHub page.
   - That includes the `.github` folder. On a Mac, press **Cmd + Shift + .** in Finder to show hidden folders.
5. Scroll down and click **Commit changes**.
6. Click the **Actions** tab. A build called **Build MUCH Launcher** starts on its own and takes about 4 to 6 minutes.
   A green check means it's done.
   - If the Actions tab asks you to enable workflows, click the green button, then **Build MUCH Launcher** → **Run workflow**.
7. Your download link is now:

   `https://github.com/YOUR-GITHUB-NAME/much-launcher/releases/latest/download/much-launcher.apk`

   Replace `YOUR-GITHUB-NAME` with your GitHub username.
   Tip: shorten it with a free link shortener (like tinyurl.com) so it's quicker to type with the remote.

**If step 6 shows no build:** the `.github` folder didn't upload. Click **Add file → Create new file**, name it
`.github/workflows/build.yml`, paste in the contents of that file from the zip, and click **Commit changes**.

## Part 2: Install it on the Fire TV Stick (about 5 minutes)

1. On the stick: **Settings → My Fire TV → Developer options**.
   - No Developer options? Go to **Settings → My Fire TV → About**, press Select on your device name **7 times**, then go back.
2. Turn on **Apps from Unknown Sources** (or **Install unknown apps**, then turn it on for **Downloader**).
3. Search for **Downloader** (the orange app by AFTVnews) and install it.
4. Open Downloader, type your download link from Part 1, step 7, and press **Go** → **Install** → **Open**.
5. MUCH Launcher is now in **Your Apps & Channels**. Move it to the front of your apps row so it's one click away.

## How it behaves

- **Opens on startup.** After the stick restarts, MUCH Launcher comes up by itself.
- **Home button.** Amazon doesn't let other apps take over the Home button, so Home still shows Amazon's screen. MUCH Launcher is one click away in your apps row.
- **Remote.** Left/Right moves between tiles, Down goes to Start, Select opens, Back closes the Start menu.
- **Start menu** lists every app installed on the stick.
- **GBTV** is found by its name, so it works whatever the app's internal ID is.

## Updating the design later

When the design changes, replace `app/src/main/assets/index.html` in your GitHub repository
(open the file → **...** → **Delete file**, then **Add file → Upload files** with the new one).
GitHub rebuilds the app automatically. Then open Downloader on the stick and use the **same link**. It installs over the old version.

## About the signing key

`app/much-launcher.keystore` signs the app so every new build installs as an update. It only matters for this one personal app.
