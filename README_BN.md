# Ludo Remote V1 — Controller + Helper (Android source)

এটি দুইটি আলাদা Android app-এর **source code starter**: `Helper` (যে ফোনে গেম চলবে) এবং `Controller` (যে ফোন থেকে ম্যানুয়াল ট্যাপ পাঠানো হবে)। এটি এখনো APK ফাইল নয় এবং এখানে Admin Panel-এর লাইসেন্স সংযোগ যুক্ত করা হয়নি।

## বর্তমান V1-এ আছে
- একই Wi-Fi/Hotspot-এ Helper-এর local HTTP server (port 18765)
- Helper স্ক্রিনে ৬ সংখ্যার অস্থায়ী Pair PIN এবং IP দেখায়
- Controller PIN দিয়ে pair করে session token পায়
- Controller থেকে normalized X/Y coordinate দিয়ে একটি tap পাঠায়
- Helper-এ Android Accessibility Service ব্যবহারকারীর অনুমতিতে tap gesture পাঠায়

## সেটআপ
1. Android Studio-তে `Helper` ফোল্ডার খুলে Gradle Sync ও Build APK(s) করুন।
2. Helper ফোনে APK install করুন। অ্যাপ খুলে `Enable Accessibility` চাপুন এবং `Ludo Helper Touch Service` চালু করুন।
3. Helper অ্যাপ খোলা রাখুন। স্ক্রিনে দেখানো IP ও PIN লিখে রাখুন।
4. `Controller` ফোল্ডার Android Studio-তে খুলে APK build/install করুন।
5. দুই ফোন একই Wi-Fi/Hotspot-এ রেখে Controller-এ Helper IP ও PIN দিন, `CONNECT / PAIR` চাপুন।
6. প্রথম পরীক্ষায় কোনো গুরুত্বপূর্ণ গেম বোতাম নয়—নিজের অনুমতিতে নিরাপদ স্ক্রিন-এলাকায় X/Y দিয়ে tap পরীক্ষা করুন। পরে গেমের UI অনুযায়ী coordinate calibrate করতে হবে।

## সীমাবদ্ধতা ও নিরাপত্তা
- এই V1 কেবল ম্যানুয়াল tap relay; dice outcome, game state, memory বা server পরিবর্তন করে না।
- Local network traffic এই prototype-এ TLS-encrypted নয়; তাই কেবল নিজের private Wi-Fi/Hotspot-এ ব্যবহার করুন। PIN/token অস্থায়ী এবং production security নয়।
- Helper অ্যাপটি সামনে খোলা রাখতে হবে; Android background restrictions বা ফোনভেদে server থেমে যেতে পারে।
- Accessibility চালু করা স্পষ্ট ব্যবহারকারী-সম্মতিতে করতে হবে। Android permission/Accessibility সীমাবদ্ধতার কারণে সব ডিভাইসে একই ফল নিশ্চিত নয়।
- APK build করতে Android Studio, Android SDK এবং internet-এ Gradle dependencies লাগবে। এই পরিবেশে APK build/test করা হয়নি।
- Existing Ludo Controller APK-এর লাইসেন্স/সার্ভার এই source-এর অংশ নয়। নতুন অ্যাপের জন্য আলাদা নিজস্ব license API ও Admin Panel integration পরবর্তী ধাপ।
