package com.letitbee.diamondvaluationsystem.service;

import com.letitbee.diamondvaluationsystem.payload.AccountDTO;

public interface AccountService {
    AccountDTO getALlAccounts();
    AccountDTO getAccountById(Long id);
}
