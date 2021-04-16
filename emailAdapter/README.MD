# Email sender

## Setup

In order to get the email sender to work, it needs to be configured properly. A configuration file called `EmailProperties.cfg` will be generated when the `notima-email` feature is installed. The file should be configured as follows:

    emailHost = {hostname to your email server}
    emailUser = {email address top send emails from}
    emailPass = {password for email user on mail server}
    senderPrivateKey = {path to private pgp key of sender}
    senderPublicKey = {path to public pgp key of sender}
    senderPrivateKeyPassword = {password for private key of sender}

karaf needs to be rebooted (or this bundle reloaded) in order for the new configuration to take effect.

## Key pair generation

A key pair can easily be generated using the command `notima:generate-keypair` in order to be used as sender keys.

    DESCRIPTION
        notima:generate-keypair

        Generates GPG keypair for email encryption. Default values from EmailProperties.cfg will be used if flags are omitted.

    SYNTAX
        notima:generate-keypair [options]

    OPTIONS
        -pa, --private-key-password
                Override the private key password.
        --help
                Display this help message
        -u, --user-id
                Override the user id.
        -pr, --private-key-file
                Override the private key file location.
        -pu, --public-key-file
                Override the public key file location.

No options need to be provided if `EmailProperties.cfg` is properly configured.

## Using the Email sender

An instance of the `PGPEmailMessageSender` service can be retrieved from a `MessageSenderFactory` as follows:

    MessageSender emailSender = messageSenderFactoryInstance.getMessageSender("email");

A message is sent by calling `PGPEmailMessageSender.send(Message message, KeyManager keyManager, boolean attachSenderPublicKey);`. If encryption is desired, encryption keys can be provided in two ways. The recommended way is to pass an instance of a keyManager that can provide the public key for the recipient. In this case, the `recipientPublicKey` of the message should be null. A key can also be provided in the message object. In this case, the `keyManager` parameter will not be used by the `PGPEmailMessageSender` and a null value can therefore be passed.