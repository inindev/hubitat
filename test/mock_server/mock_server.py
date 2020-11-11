#!/usr/bin/env python3
#
#  mock_server.py
#
#    Mock service to simulate the Honeywell WiFi 9000 Thermostat REST server
#
#    Copyright 2000 John Clark
#
#    Licensed under the Apache License, Version 2.0 (the "License");
#    you may not use this file except in compliance with the License.
#    You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
#    Unless required by applicable law or agreed to in writing, software
#    distributed under the License is distributed on an "AS IS" BASIS,
#    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#    See the License for the specific language governing permissions and
#    limitations under the License.
#

import socket, socketserver
from datetime import datetime, timedelta
import re


mock_files = {
    'GET /portal/': 'mocks/portal_get.mock',
    'POST /portal/': 'mocks/portal_post.mock',
    'POST /portal/Device/GetZoneListData?locationId=2468357&page=1': 'mocks/portal_gzld_post.mock',
    'POST /portal/Location/GetLocationListData?page=1': 'mocks/portal_glld_post.mock',
}

#hl_names = re.compile(r'^(.*?[:]?)(?=[ ])', re.MULTILINE)
#hl_names = re.compile(r'^(POST|GET|PUT|PATCH|DELETE|.*?:)', re.MULTILINE)
hl_names = re.compile(r'^(.*?:)', re.MULTILINE)


class HttpRequestHandler(socketserver.BaseRequestHandler):
    def setup(self):
        print('\033[34m\n──────────────────────────────────────────────────────────────────────\033[0m')
        print('client connect: \033[32m{}\033[0m'.format(self.request.getpeername()[0]))

    def handle(self):
        request = self.request.recv(1024).decode('UTF-8')
        print(colorize_headers(request, 'request'))
        response = get_mock_response(request)
        if not response:
            raise NotImplementedError
        print()
        print(colorize_headers(response, 'response'))
        self.request.send(response.encode('UTF-8'))

    def finish(self):
        print('client disconnect: \033[32m{}\033[0m'.format(self.request.getpeername()[0]))
        print('\033[34m──────────────────────────────────────────────────────────────────────\033[0m\n')
        return socketserver.BaseRequestHandler.finish(self)


def colorize_headers(data, title):
    tokens = data.replace('\r\n\r\n', '\n\n').split('\n\n')
    out = '\033[35m────────────────────[ {} headers ]────────────────────\n\033[0m'.format(title)
    out += re.sub(hl_names, '\033[35m\\1\033[0m', tokens[0])
    out += '\n\033[36m─────────────────────[ {} body ]──────────────────────\n\033[0m'.format(title)
    if len(tokens) > 1:
        out += '\033[36m{}\033[0m\n'.format(tokens[1])
    out += '\033[36m────────────────────────────────────────────────────{}\033[0m'.format('─'*len(title))
    return out


def get_web_time(minutes=0, years=0):
    val = datetime.utcnow() + timedelta(minutes=minutes)
    if years != 0:
        val = val.replace(year=val.year+years)
    return val.strftime("%a, %d-%b-%Y %H:%M:%S GMT")


def get_mock_response(request):
    pos = request.find(' ')
    if pos < 0:
        raise ValueError
    pos = request.find(' ', pos+1)
    if pos < 0:
        raise ValueError

    key = request[:pos]
    filename = mock_files.get(key)
    if not filename:
        raise LookupError

    with open(filename, 'rt') as f:
        doc = f.read().replace('\n', '\r\n')

    cwtf = {
        'now':      get_web_time(),
        'now_p2m':  get_web_time(minutes=2),
        'now_m1y':  get_web_time(years=-1),
        'now_p50y': get_web_time(years=50)
    }

    return doc.format(**cwtf)


def get_myip():
    s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    try:
        # doesn't even have to be reachable
        s.connect(('10.255.255.255', 1))
        myip = s.getsockname()[0]
    except Exception:
        iaddrp = '127.0.0.1'
    finally:
        s.close()
    return myip


def main(port=8080):
    httpd = socketserver.TCPServer(('', port), HttpRequestHandler)

    print('\nwaiting for connection...')
    print('  address: {}'.format(get_myip()))
    print('  port: {}\n'.format(port))

    try:
        httpd.serve_forever()
    except KeyboardInterrupt:
        print('\nexiting...')
        pass
    finally:
        httpd.server_close()

    print('\nhttp server closed\n')
    return(0)


if __name__ == '__main__':
    exit(main())
