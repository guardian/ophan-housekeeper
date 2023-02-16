# Housekeeper

The Ophan Dashboard lets you [configure email alerts](https://dashboard.ophan.co.uk/alerts),
which are later sent by Trigr when traffic passes the configured threshold.
When Ophan's Trigr sends those emails, some of them are bounced, either because the email has
an out of office automatic reply, or because the email no longer exists.

If too many emails bounce, Amazon will put us on 'probation' (this has happened
[before](https://github.com/guardian/ophan/issues/2765)), and then place us on a deny list, 
preventing us from sending emails altogether.

The Ophan Housekeeper is an AWS Lambda that receives these bounce emails, and for each 
hard bounce (email no longer exists) it clears all subscriptions for that email
in our DynamoDB table that keep tracks of email alerts users have set up.

### Original Code

This code was copied over from the main Ophan repository, you can see the last version of it
there at:

https://github.com/guardian/ophan/tree/last-version-of-housekeeper-in-main-ophan-repo/housekeeper

