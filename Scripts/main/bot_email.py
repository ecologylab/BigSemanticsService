#!/usr/bin/python

import smtplib
from email.mime.text import MIMEText
from simple_config import load_config

config = load_config("bot.conf")

def send_bot_email(receiver, subject, message):
  login = config['bot_email_login']
  passwd = config['bot_email_passwd']
  # receiver = "bigsemantics@ecologylab.net"

  mail_text = MIMEText(message)
  mail_text['Subject'] = subject
  mail_text['From'] = login
  mail_text['To'] = receiver

  smtp_server = smtplib.SMTP('smtp.gmail.com', 587)
  smtp_server.starttls()
  smtp_server.login(login, passwd)
  problems = smtp_server.sendmail(login, [receiver], mail_text.as_string())
  quit_status = smtp_server.quit()
  return (quit_status, problems)

def send_bot_email_to_maintainers(subject, message):
  return send_bot_email(config["receiver"], subject, message)

