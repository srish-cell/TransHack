import argparse
import socketio
from termcolor import colored  # For color-coding
from pyfiglet import Figlet  # For creating ASCII art text
from tool import anomaly_model

# Function to display the TransHack banner
def display_banner():
    figlet = Figlet(font='slant')  # You can choose a different font style
    banner_text = figlet.renderText('TransHack')
    print(colored(banner_text, 'blue'))  # Color the banner text

# Parse command line arguments
parser = argparse.ArgumentParser(description='Live Transaction Monitor')
parser.add_argument('server_url', help='URL of the Flask app')
args = parser.parse_args()

# Display the TransHack banner
display_banner()

# Socket.IO Client
sio = socketio.Client()

@sio.event
def connect():
    print('Connected to server')

@sio.event
def transaction_event(data):
    account_type = data.get('account_type')  # Access account type
    user_account_number = data.get('user_account_number')  # Access user account number
    destination_account_number = data.get('destination_account_number')  # Access destination account number

    # Check for large transfers from non-corporate accounts
    if data['amount'] > 5000000 and account_type != 'corporate':
          # Mark as anomalous
        color = 'red'  # Color for anomalous transaction
        print(colored("ALERT: Large transaction from a non-corporate account!", 'red'))  # Report anomaly
    else:
          # Not anomalous
        color = 'green'  # Color for normal transaction

    # Display transaction information with color-coding, including account numbers
    transaction_info = f"Transaction: {data['transaction_id']} ({data['amount']}, {data['timestamp']})"
    if user_account_number:
        transaction_info += f" ({user_account_number})"
    if destination_account_number:
        transaction_info += f" to {destination_account_number}"
    print(colored(transaction_info, color))

# Connect to the server and wait
sio.connect(args.server_url)
sio.wait()
