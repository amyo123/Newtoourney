import { initializeApp } from "firebase/app";
import { getStorage, ref, uploadString } from "firebase/storage";

const firebaseConfig = {
  apiKey: "AIzaSyBRICsMK_l09oTtBF9mOoFr5rFRDMMp01k",
  projectId: "esportstournaments-9743b",
  storageBucket: "esportstournaments-9743b.appspot.com",
  appId: "1:776259295599:android:2aa5bb3ca51e58faadb09a"
};

const app = initializeApp(firebaseConfig);
const storage = getStorage(app);
const storageRef = ref(storage, 'testfile.txt');

uploadString(storageRef, 'Hello World').then((snapshot) => {
  console.log('Uploaded!');
  process.exit(0);
}).catch((error) => {
  console.error("Upload failed: ", error);
  process.exit(1);
});
