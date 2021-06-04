package com.motorbesitzen.rolewatcher.data.repo;

import com.motorbesitzen.rolewatcher.data.dao.LinkingInformation;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface LinkingInformationRepo extends CrudRepository<LinkingInformation, Long> {
	Optional<LinkingInformation> findByVerificationCode(String verificationCode);
}
