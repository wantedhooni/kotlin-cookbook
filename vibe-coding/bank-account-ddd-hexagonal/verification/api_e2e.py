#!/usr/bin/env python3
"""Live HTTP E2E against compose Keycloak and a running Spring Boot API (stdlib only)."""
import base64
import json
import os
import sys
import uuid
from decimal import Decimal
from urllib.error import HTTPError, URLError
from urllib.parse import urlencode
from urllib.request import Request, urlopen

API = os.getenv('E2E_API_URL', 'http://localhost:8080').rstrip('/')
OIDC = os.getenv('E2E_KEYCLOAK_URL', 'http://localhost:8081').rstrip('/')


def call(url, method='GET', data=None, token=None, form=False):
    payload = None if data is None else (urlencode(data).encode() if form else json.dumps(data).encode())
    headers = {'Content-Type': 'application/x-www-form-urlencoded' if form else 'application/json'}
    if token:
        headers['Authorization'] = 'Bearer ' + token
    req = Request(url, data=payload, headers=headers, method=method)
    try:
        with urlopen(req, timeout=15) as response:
            raw = response.read()
            return response.status, json.loads(raw) if raw else None
    except HTTPError as exc:
        body = exc.read()
        try:
            response = json.loads(body)
        except ValueError:
            response = body.decode(errors='replace')
        return exc.code, response


def require(condition, message):
    if not condition:
        raise AssertionError(message)


def token(username, password):
    status, result = call(OIDC + '/realms/bank/protocol/openid-connect/token', 'POST', {
        'grant_type': 'password', 'client_id': 'bank-api', 'username': username, 'password': password
    }, form=True)
    require(status == 200 and 'access_token' in result, f'Keycloak login {username}: {status} {result}')
    raw = result['access_token'].split('.')[1]
    claims = json.loads(base64.urlsafe_b64decode(raw + '=' * (-len(raw) % 4)))
    require('bank-api' in claims.get('aud', []), f'JWT missing bank-api audience: {claims.get("aud")}')
    return result['access_token'], claims['sub']


def api(path, method='GET', data=None, bearer=None):
    return call(API + path, method, data, bearer)


def check_status(path, method, data, bearer, expected):
    actual, obj = api(path, method, data, bearer)
    require(actual == expected, f'{method} {path}: expected {expected}, got {actual}: {obj}')
    return obj


def run():
    print('E2E: health')
    check_status('/actuator/health', 'GET', None, None, 200)
    check_status('/api/v1/accounts', 'GET', None, None, 401)
    alice, alice_sub = token('alice', 'alice-dev-password')
    bob, bob_sub = token('bob', 'bob-dev-password')
    print('E2E: real Keycloak access tokens issued for alice and bob')
    suffix = uuid.uuid4().hex[:12]
    a = check_status('/api/v1/accounts', 'POST', {'bankCode': 'BANK001', 'accountNumber': 'E2EA-' + suffix, 'ownerName': alice_sub}, alice, 201)
    b = check_status('/api/v1/accounts', 'POST', {'bankCode': 'BANK001', 'accountNumber': 'E2EB-' + suffix, 'ownerName': bob_sub}, bob, 201)
    aid, bid = a['id'], b['id']
    check_status(f'/api/v1/accounts/{aid}', 'GET', None, bob, 403)
    check_status('/api/v1/accounts', 'POST', {'bankCode': 'BANK001', 'accountNumber': 'INVALID-' + suffix, 'ownerName': bob_sub}, alice, 403)
    print('E2E: account creation and cross-account access restrictions')
    deposit = check_status(f'/api/v1/accounts/{aid}/deposits', 'POST', {'amount': '100.00'}, alice, 200)
    require(Decimal(str(deposit['balance'])) == Decimal('100.00'), 'deposit balance mismatch')
    withdraw = check_status(f'/api/v1/accounts/{aid}/withdrawals', 'POST', {'amount': '10.00'}, alice, 200)
    require(Decimal(str(withdraw['balance'])) == Decimal('90.00'), 'withdraw balance mismatch')
    check_status(f'/api/v1/accounts/{aid}/withdrawals', 'POST', {'amount': '1.00'}, bob, 403)
    print('E2E: deposit / withdrawal / authorization')
    transfer = {'requestId': str(uuid.uuid4()), 'fromAccountId': aid, 'toAccountId': bid, 'amount': '25.00'}
    first = check_status('/api/v1/accounts/transfers', 'POST', transfer, alice, 200)
    second = check_status('/api/v1/accounts/transfers', 'POST', transfer, alice, 200)
    require(first['referenceId'] == second['referenceId'], 'idempotency reference mismatch')
    require(second['replayed'] is True, 'transfer replay flag missing')
    check_status('/api/v1/accounts/transfers', 'POST', {**transfer, 'amount': '26.00'}, alice, 409)
    check_status('/api/v1/accounts/transfers', 'POST', {**transfer, 'requestId': str(uuid.uuid4())}, bob, 403)
    a_state = check_status(f'/api/v1/accounts/{aid}', 'GET', None, alice, 200)
    b_state = check_status(f'/api/v1/accounts/{bid}', 'GET', None, bob, 200)
    require(Decimal(str(a_state['balance'])) == Decimal('65.00'), 'sender final balance mismatch')
    require(Decimal(str(b_state['balance'])) == Decimal('25.00'), 'receiver final balance mismatch')
    history = check_status(f'/api/v1/accounts/{aid}/transactions', 'GET', None, alice, 200)
    require(len(history) >= 3, f'expected deposit, withdrawal, transfer in history: {history}')
    print('E2E: transfer / idempotency / history / final balances / access restrictions PASS')
    print('HTTP END-TO-END PASS')


if __name__ == '__main__':
    try:
        run()
    except (AssertionError, URLError, KeyError, ValueError) as exc:
        print('HTTP END-TO-END FAIL:', exc, file=sys.stderr)
        sys.exit(1)
