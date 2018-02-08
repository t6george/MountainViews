
![mountain views showcase stacked 3](https://user-images.githubusercontent.com/29645585/35462682-f5533b98-02ba-11e8-8245-56d4febd1822.png)

# Mountain Views
Mountain Views is a social media and backcountry safety platform based on Android and the Web. On both Android and Web apps, users may post pictures, read and write trip reports, plan trips, and track locations of mutual followed users currently on trips. The Android application is paired with the Myo Armband, meant to be worn when on trips and if the unexpected happens and the user is in desperate need of rescue, they perform a preset sequence of gestures to automatically send the user's coordinates to local rescue authorities, family members, and followers through push, email, and text notifications.

Mountain Views was started at PennApps XVII and can be found on Devpost [here.](https://devpost.com/software/mountain-views)

Interconnecting our Android and Web apps, we strongly relied on the use of Amazon Web Services. While linking the two platforms, we ended up using AWS DynamoDB and S3 for our database and file storage, Cognito for user authentication, SNS for everything notification related, and EC2 for our dynamic website. In terms of the web app, it was developed using HTML5, CSS3, as well JavaScript for the more variable portions including the login page. In order to integrate the backend, we used Node.js and Python to particularly implement DynamoDB and S3. On the Android side of things, the whole entirety of the web app and more was also implemented on the Android app with the use of Java and Google Play Services.
