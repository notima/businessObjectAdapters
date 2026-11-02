# Email sender

## Setup

In order to get the email sender to work, it needs to be configured properly. A configuration file called `EmailProperties.cfg` will be generated when the `notima-email` feature is installed. The file should be configured as follows:

    emailHost = {hostname to your email server}
    emailPort = {25 (default), 587 with emailStartTls, 465 with emailSsl}
    emailUser = {email address to send emails from, also the SMTP login}
    emailPass = {password for email user on mail server, empty: no SMTP authentication}
    emailName = {display name of the sender, optional}
    emailStartTls = {true: STARTTLS required (port 587), default false}
    emailSsl = {true: TLS from the start (port 465), default false}
    senderPrivateKey = {path to private pgp key of sender}
    senderPublicKey = {path to public pgp key of sender}
    senderPrivateKeyPassword = {password for private key of sender}

## PGP

Signed and encrypted mails are PGP/MIME (RFC 3156), as read by Thunderbird, Outlook (GpgOL, gpg4o),
Apple Mail (GPG Suite), mutt and others:

- signed: `multipart/signed`, detached signature (SHA-256) with the sender's private key
- encrypted: `multipart/encrypted` to the recipient's public key (AES-256, integrity protected, compressed)
- signed and encrypted: the signed mail, encrypted. The subject is not encrypted.

Attachments and the attached sender public key are inside the signed and encrypted part. Without a
recipient key or with a wrong passphrase nothing is sent (`MessageSenderException`).

Only Bouncy Castle (`bcpg-jdk18on`, `bcprov-jdk18on`, its lightweight API, no JCE provider to register)
and JavaMail are used, both from Maven Central and embedded in the bundle.

Keys are ASCII-armored or binary OpenPGP files, RSA, ed25519/cv25519 and others, e.g. from GnuPG:

    gpg --export --armor sender@example.com > sender.pub.asc
    gpg --export-secret-keys --armor sender@example.com > sender.sec.asc   # chmod 600

The recipient key is taken from the message (`recipientPublicKey`) or else from the `KeyManager`
(by the recipient's email address). A key file with several keys: the one with the recipient's
address in a user id. Revoked and expired keys are not used.

The tests send through an SMTP server (GreenMail) and check what arrived, also with GnuPG if installed.

karaf needs to be rebooted (or this bundle reloaded) in order for the new configuration to take effect.

## Key pair generation

A key pair can easily be generated using the command `notima:generate-keypair` in order to be used as sender keys
(RSA 3072, a signing primary key and an encryption subkey, the private key protected with the passphrase
and readable only by its owner). Existing key files are not overwritten.

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