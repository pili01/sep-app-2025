# POKRETANJE APLIKACIJE

<span style="color: red; font-size: 24px;">Ctrl+Shift+B</span>

# Poslati sledece zahtjeve:

```
1.
registrovati korisnika i admina (webShopRegister): https://localhost:8441/auth/register
u bazi izmjeniti za admina role AUTHOR

2.
registrovati admina za psp (psp register admin): https://localhost:8442/auth/register
```

### 3. Dodati Merchanta u psp

Urlovi za web-shop

```
error: https://localhost:4201/payment-error
faild: https://localhost:4201/payment-failed
success: https://localhost:4201/payment-success
webHook: https://localhost:8441/api/webhook/payment-notification
```

## Merchant Id:

d57312ec-84ee-41e9-bb51-c5f762ae9b8d
kv6Wu/oh1iLJUA6qO4zUDZqMJdr+QTzfeirKiybMsz3P/4gtf35wQ/DA4QJPFybmFuRvcsh0x2BcdBwiXT/gAw==

## Url za payment method:

```
health: https://localhost:8444/api/health
hostname: https://localhost:8444
paymentEndPoint: https://localhost:8444/api/payment/pay
```

### Pay Pal config string koji se postavlja kada se dodaje payPal metod placanja

```
{
  "clientId": "AT476MzyX-JeEzzQbDavJEgm9HHbbtaXqHE3o2j2yUtMv2iByGWwwb53VzXG6FzkobYj9rkaoG1690pQ",
  "secret": "EH39JHk_Sr5X932MnuwfrllTqSrEXzTZNGrQ_SANgwfdyzaPqLW6KsCiwZz5F1RcNee2LTs_KADa9t2u",
  "mode": "sandbox"
}
```

## BANK secrets

```
frontend.api.url=https://localhost:4203/
psp.api.url=https://localhost:8442/
master-key=H6pZk8Zx5zR3y2PqN0mX9YwKc7VJmT4FQdA1B8eS2uM=
```

## PSP secrets

```
master-key=H6pZk8Zx5zR3y2PqN0mX9YwKc7VJmT4FQdA1B8eS2uM=
server.ssl.key-store-password=SertifikatZaSEP2025!
spring.datasource.username=postgres
spring.datasource.password=super
frontend.api.url=https://localhost:4202
bank.api.url=https://localhost:8443
webhook.url=https://localhost:8442/api/webhook/payment-notification
exchange.rate.api.url= http://v6.exchangerate-api.com/v6/7890a0954404ce6ea87aafbd/pair/
```

## PSP PayPal

```
master-key=H6pZk8Zx5zR3y2PqN0mX9YwKc7VJmT4FQdA1B8eS2uM=

server.ssl.key-store-password=SertifikatZaSEP2025!

spring.datasource.username=postgres

spring.datasource.password=super

psp.core.api.url=https://localhost:8442

psp.core.connect.endpoint=/api/payment-methods/connect

exchange.rate.api.url= http://v6.exchangerate-api.com/v6/7890a0954404ce6ea87aafbd/pair/
```

## WebShop

```
master-key=H6pZk8Zx5zR3y2PqN0mX9YwKc7VJmT4FQdA1B8eS2uM=

merchant-id=d57312ec-84ee-41e9-bb51-c5f762ae9b8d
merchant-password=6f27b642-4c3d-4cd3-9b8e-7b42857ea64c

currency = EUR

merchant-base-url=https://localhost:8442
merchant-handshake-endpoint=/api/merchants/handshake

spring.datasource.username=postgres
spring.datasource.password=super

server.ssl.key-store-password=SertifikatZaSEP2025!
```
