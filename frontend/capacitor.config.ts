import { CapacitorConfig } from '@capacitor/cli';
import { config as loadEnv } from 'dotenv';

// .env ファイルを読み込む
loadEnv();

interface CapacitorUpdaterConfig {
  autoUpdate: boolean;
  updateUrl: string;
}

const updaterUrl = process.env.CAPACITOR_UPDATER_URL;
const plugins: CapacitorConfig['plugins'] = {
  SplashScreen: {
    // launchShowDurationを削除し、main.tsxで明示的にhide()を呼び出して制御
    backgroundColor: '#ffffff',
    showSpinner: false,
  },
  StatusBar: {
    style: 'LIGHT',
    backgroundColor: '#ffffff',
  },
};

if (updaterUrl) {
  (plugins as CapacitorConfig['plugins'] & { CapacitorUpdater?: CapacitorUpdaterConfig }).CapacitorUpdater = {
    autoUpdate: true,
    updateUrl: updaterUrl,
  };
}

const config: CapacitorConfig = {
  appId: 'com.egograph.app',
  appName: 'EgoGraph',
  webDir: 'dist',
  server: {
    androidScheme: 'https',
  },
  plugins,
};

export default config;
